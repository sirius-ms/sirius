package de.unijena.bioinf.lcms.deconvolution;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the candidate edges of a channel into one {@link FragmentAssignment} per fragment.
 * <p>
 * The graph is bipartite: a fragment is connected to the precursor features it could belong to and to nothing else.
 * A fragment's probability of belonging to a candidate is therefore that candidate's weight as a share of everything
 * the fragment could be, including the "belongs to none of them" alternative:
 * <pre>
 *     p(fragment -&gt; precursor)  =  weight(precursor) / (sum of all weights + unassigned weight)
 * </pre>
 * The unassigned weight is what keeps a noise fragment from being confidently assigned: without it every fragment
 * would have to distribute all of its probability over its candidates however weak the evidence, and this mirrors the
 * {@code PrecursorIonType.unknown()} entry an {@code AdductAssignment} carries.
 * <p>
 * This used to be computed by an absorbing random walk over a graph that also had fragment-to-fragment edges, so that
 * a fragment torn between two co-eluting precursors could be settled by an unambiguous sibling. Those edges were
 * never produced - every connected component was a single fragment on every real sample - and for a single transient
 * node with no neighbours the walk's {@code (I - P_QQ)^-1 P_QA} is exactly the division above. The solver, the
 * component search and the choice between an exact and an iterative solution were computing nothing that this does
 * not, so they are gone; the history has them if the sibling idea is ever picked up again.
 */
public class DiaFragmentResolver {

    /**
     * Weight of the "belongs to none of them" alternative. A candidate has to be better than this to take
     * probability away from it, so it sets how much evidence is needed before a fragment is assigned at all. It is on
     * the same scale as the edge weights, i.e. units of log p-value beyond the significance floor.
     */
    public static final double DEFAULT_UNASSIGNED_WEIGHT = 1d;

    /**
     * A log p-value of zero means no evidence, and has to translate to no weight rather than to a weight of one.
     * Everything below the floor is treated as no evidence at all; the floor of three matches the cutoff
     * {@code AdductNetwork.deleteEdgesWithLowPvalue} applies, which is roughly a p-value of 0.05.
     */
    public static final double DEFAULT_SIGNIFICANCE_FLOOR = 3d;

    /**
     * @param logPValue natural log of a p-value, at most zero, where zero means no evidence
     * @param floor     how significant an edge has to be before it carries any weight at all
     * @return a non-negative weight, zero for anything not beyond the floor
     */
    public static double weightFromLogPValue(double logPValue, double floor) {
        if (!Double.isFinite(logPValue)) return 0d;
        return Math.max(0d, -logPValue - floor);
    }

    /**
     * How much a candidate edge is worth. Kept as a parameter because the calibrations that turn correlation and
     * specificity into p-values live outside this class.
     */
    public interface EdgeWeighting {
        double weight(DiaAssignmentGraph.PrecursorEdge edge);
    }

    /**
     * How much MS2 signal a precursor can account for, in the units of its MS1 intensity.
     * <p>
     * This is the second half of the evidence and it is not interchangeable with a threshold on intensity. Ion
     * count is conserved for singly charged precursors: each precursor ion is detected either intact or as exactly
     * one charged fragment, so a precursor of MS1 intensity I can only explain about {@code k*I} of MS2 ion
     * current. Among candidates whose elution profiles agree equally well, the odds that a fragment came from one
     * of them are therefore proportional to its share of the ion flux into that window - a precursor a hundred
     * times more intense than its competitors is a hundred times more likely to have emitted the fragment,
     * whatever the shapes say.
     * <p>
     * Deleting weak precursors instead would be wrong in a way this is not. A weak precursor that is the
     * <em>only</em> candidate for a fragment keeps it, because the comparison is relative: capacities are
     * normalised within each fragment's own candidate set, so a lone candidate always has a factor of one. What
     * this cannot do on its own is stop an intense precursor absorbing more than it could physically have emitted;
     * that is what {@link FragmentIntensityShares} is for.
     */
    public interface PrecursorCapacity {
        double capacityOf(int precursorIndex);
    }

    /** every precursor is equally able to explain a fragment, i.e. intensity is ignored */
    public static final PrecursorCapacity UNIFORM_CAPACITY = index -> 1d;

    /**
     * A candidate edge whose weight has already been decided.
     * <p>
     * The resolver does not care what a precursor or a fragment is, only how strongly they are connected, which is
     * what lets it serve both stages. Stage one's nodes are the features and traces of a single sample; stage two's
     * are features and fragments aligned across samples, whose evidence is pooled before it gets here.
     */
    public record WeightedEdge(int precursorIndex, int fragmentIndex, double weight) {
    }

    @Getter
    public static class Result {
        /** one per fragment of the graph, in the graph's fragment order */
        private final FragmentAssignment[] assignments;

        Result(FragmentAssignment[] assignments) {
            this.assignments = assignments;
        }

        /** @return number of fragments that were assigned to at least one precursor */
        public int assignedFragments() {
            int count = 0;
            for (FragmentAssignment a : assignments) if (!a.isEmpty()) ++count;
            return count;
        }

        /** @return number of fragments a single sample could not attribute to one precursor */
        public int ambiguousFragments() {
            int count = 0;
            for (FragmentAssignment a : assignments) if (a.isAmbiguous()) ++count;
            return count;
        }
    }

    public static Result resolve(DiaAssignmentGraph graph, EdgeWeighting weighting) {
        return resolve(graph, weighting, UNIFORM_CAPACITY, DEFAULT_UNASSIGNED_WEIGHT,
                FragmentAssignment.DEFAULT_MIN_PROBABILITY);
    }

    public static Result resolve(DiaAssignmentGraph graph, EdgeWeighting weighting, double unassignedWeight,
                                 double minProbability) {
        return resolve(graph, weighting, UNIFORM_CAPACITY, unassignedWeight, minProbability);
    }

    public static Result resolve(DiaAssignmentGraph graph, EdgeWeighting weighting, PrecursorCapacity capacity,
                                 double unassignedWeight, double minProbability) {
        final List<WeightedEdge> weighted = new ArrayList<>(graph.getPrecursorEdges().size());
        for (DiaAssignmentGraph.PrecursorEdge edge : graph.getPrecursorEdges()) {
            weighted.add(new WeightedEdge(edge.precursorIndex(), edge.fragmentIndex(), weighting.weight(edge)));
        }
        return resolveWeighted(graph.getFragments().size(), weighted, capacity, unassignedWeight, minProbability);
    }

    /**
     * The same, for edges that already carry their weight.
     *
     * @param fragmentCount number of fragments; the edges index into {@code 0..fragmentCount-1}
     */
    public static Result resolveWeighted(int fragmentCount, List<WeightedEdge> edges, PrecursorCapacity capacity,
                                         double unassignedWeight, double minProbability) {
        final FragmentAssignment[] assignments = new FragmentAssignment[fragmentCount];
        java.util.Arrays.fill(assignments, FragmentAssignment.unassigned());

        final List<List<WeightedEdge>> candidates = new ArrayList<>(fragmentCount);
        for (int k = 0; k < fragmentCount; ++k) candidates.add(new ArrayList<>());
        for (WeightedEdge edge : edges) {
            if (edge.weight() > 0) candidates.get(edge.fragmentIndex()).add(edge);
        }

        for (int fragment = 0; fragment < fragmentCount; ++fragment) {
            final List<WeightedEdge> mine = candidates.get(fragment);
            if (mine.isEmpty()) continue;
            // Capacities are normalised within this fragment's own candidate set, so the best supported candidate
            // keeps its full weight and the unassigned alternative stays comparable. Only the ratios between
            // candidates matter, and those are preserved.
            double largestCapacity = 0d;
            for (WeightedEdge edge : mine) {
                largestCapacity = Math.max(largestCapacity, capacity.capacityOf(edge.precursorIndex()));
            }
            final IntArrayList precursors = new IntArrayList(mine.size());
            final double[] weights = new double[mine.size()];
            double total = Math.max(0d, unassignedWeight);
            for (int k = 0; k < mine.size(); ++k) {
                final WeightedEdge edge = mine.get(k);
                final double share = largestCapacity > 0
                        ? Math.max(0d, capacity.capacityOf(edge.precursorIndex())) / largestCapacity
                        : 1d;
                weights[k] = edge.weight() * share;
                precursors.add(edge.precursorIndex());
                total += weights[k];
            }
            if (!(total > 0d)) continue;  // nothing this fragment could be, not even unassigned
            for (int k = 0; k < weights.length; ++k) weights[k] /= total;
            assignments[fragment] = FragmentAssignment.of(precursors.toIntArray(), weights,
                    Math.max(0d, unassignedWeight) / total, minProbability);
        }
        return new Result(assignments);
    }
}
