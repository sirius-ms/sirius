package de.unijena.bioinf.lcms.deconvolution;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Turns the candidate edges of a channel into one {@link FragmentAssignment} per fragment.
 * <p>
 * Every fragment is a transient node of an {@link AbsorbingWalk}, every precursor feature an absorbing one, plus
 * one absorbing node standing for "belongs to none of them" that every fragment is connected to. Without that
 * node the walk would have to distribute all of a fragment's probability over the candidates, however weak the
 * evidence, and a noise fragment would be assigned with confidence.
 * <p>
 * Edges between fragments are supported but are not produced by {@link DiaAssignmentGraph} yet. Note what they
 * are for: a fragment that is too sparsely measured to be correlated with a precursor is equally too sparsely
 * measured to be correlated with a sibling fragment, so they are no help there. Their use is disambiguation - a
 * fragment torn between two co-eluting precursors can be settled by an unambiguous sibling. Because they connect
 * fragments, they are also the only thing that makes a component larger than a single fragment, which is why the
 * component machinery below matters even though it currently always sees components of size one.
 * <p>
 * The solver is chosen per component: exact below {@link #DEFAULT_EXACT_SOLVER_LIMIT} fragments, iterative above
 * it, because the exact solver materialises a dense matrix over a component's fragments. The distribution of
 * component sizes is reported, so a real run can tell us which regime we are actually in rather than us guessing.
 */
public class DiaFragmentResolver {

    /** above this many fragments in one component the exact solver's dense matrix gets out of hand */
    public static final int DEFAULT_EXACT_SOLVER_LIMIT = 250;

    /** steps of the truncated walk used above that limit */
    public static final int DEFAULT_ITERATIVE_STEPS = 12;

    /**
     * Weight of the "belongs to none of them" node. An edge has to be better than this to take probability away
     * from it, so it sets how much evidence is needed before a fragment is assigned at all. It is on the same
     * scale as the edge weights, i.e. units of log p-value beyond the significance floor.
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
     * that needs the total to be capped at its capacity, which is a constraint over sets of edges rather than a
     * per edge weight.
     */
    public interface PrecursorCapacity {
        double capacityOf(int precursorIndex);
    }

    /** every precursor is equally able to explain a fragment, i.e. intensity is ignored */
    public static final PrecursorCapacity UNIFORM_CAPACITY = index -> 1d;

    /** an edge between two fragments, see the note in the class comment */
    public record FragmentEdge(int leftFragment, int rightFragment, double weight) {
    }

    /**
     * A candidate edge whose weight has already been decided.
     * <p>
     * The walk itself does not care what a precursor or a fragment is, only how strongly they are connected, which
     * is what lets the same solver resolve both stages. Stage one's nodes are the features and traces of a single
     * sample; stage two's are features and fragments aligned across samples, whose evidence is pooled before it
     * gets here.
     */
    public record WeightedEdge(int precursorIndex, int fragmentIndex, double weight) {
    }

    @Getter
    public static class Result {
        /** one per fragment of the graph, in the graph's fragment order */
        private final FragmentAssignment[] assignments;
        /** number of fragments per connected component */
        private final int[] componentSizes;
        private final int componentsSolvedExactly;
        private final int componentsSolvedIteratively;

        Result(FragmentAssignment[] assignments, int[] componentSizes, int componentsSolvedExactly,
               int componentsSolvedIteratively) {
            this.assignments = assignments;
            this.componentSizes = componentSizes;
            this.componentsSolvedExactly = componentsSolvedExactly;
            this.componentsSolvedIteratively = componentsSolvedIteratively;
        }

        public int largestComponent() {
            int max = 0;
            for (int size : componentSizes) max = Math.max(max, size);
            return max;
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
        return resolve(graph, weighting, UNIFORM_CAPACITY, List.of(), DEFAULT_UNASSIGNED_WEIGHT,
                FragmentAssignment.DEFAULT_MIN_PROBABILITY, DEFAULT_EXACT_SOLVER_LIMIT, DEFAULT_ITERATIVE_STEPS);
    }

    public static Result resolve(DiaAssignmentGraph graph, EdgeWeighting weighting, List<FragmentEdge> fragmentEdges,
                                 double unassignedWeight, double minProbability, int exactSolverLimit,
                                 int iterativeSteps) {
        return resolve(graph, weighting, UNIFORM_CAPACITY, fragmentEdges, unassignedWeight, minProbability,
                exactSolverLimit, iterativeSteps);
    }

    public static Result resolve(DiaAssignmentGraph graph, EdgeWeighting weighting, PrecursorCapacity capacity,
                                 List<FragmentEdge> fragmentEdges, double unassignedWeight, double minProbability,
                                 int exactSolverLimit, int iterativeSteps) {
        final List<WeightedEdge> weighted = new ArrayList<>(graph.getPrecursorEdges().size());
        for (DiaAssignmentGraph.PrecursorEdge edge : graph.getPrecursorEdges()) {
            weighted.add(new WeightedEdge(edge.precursorIndex(), edge.fragmentIndex(), weighting.weight(edge)));
        }
        return resolveWeighted(graph.getFragments().size(), weighted, capacity, fragmentEdges, unassignedWeight,
                minProbability, exactSolverLimit, iterativeSteps);
    }

    /**
     * The same, for edges that already carry their weight.
     *
     * @param fragmentCount number of fragments; the edges index into {@code 0..fragmentCount-1}
     */
    public static Result resolveWeighted(int fragmentCount, List<WeightedEdge> edges, PrecursorCapacity capacity,
                                         List<FragmentEdge> fragmentEdges, double unassignedWeight,
                                         double minProbability, int exactSolverLimit, int iterativeSteps) {
        final FragmentAssignment[] assignments = new FragmentAssignment[fragmentCount];
        java.util.Arrays.fill(assignments, FragmentAssignment.unassigned());

        // candidate edges per fragment, and fragment neighbours, both by fragment index
        final List<List<WeightedEdge>> candidates = new ArrayList<>(fragmentCount);
        for (int k = 0; k < fragmentCount; ++k) candidates.add(new ArrayList<>());
        for (WeightedEdge edge : edges) {
            candidates.get(edge.fragmentIndex()).add(edge);
        }
        final List<IntArrayList> neighbours = new ArrayList<>(fragmentCount);
        for (int k = 0; k < fragmentCount; ++k) neighbours.add(new IntArrayList());
        for (FragmentEdge edge : fragmentEdges) {
            if (edge.weight() <= 0) continue;
            neighbours.get(edge.leftFragment()).add(edge.rightFragment());
            neighbours.get(edge.rightFragment()).add(edge.leftFragment());
        }

        final IntArrayList componentSizes = new IntArrayList();
        int exact = 0, iterative = 0;
        final BitSet visited = new BitSet(fragmentCount);
        for (int seed = 0; seed < fragmentCount; ++seed) {
            if (visited.get(seed)) continue;
            final IntArrayList component = spread(seed, neighbours, visited);
            componentSizes.add(component.size());
            if (component.size() > exactSolverLimit) ++iterative;
            else ++exact;
            solveComponent(component, candidates, fragmentEdges, capacity, unassignedWeight,
                    minProbability, exactSolverLimit, iterativeSteps, assignments);
        }
        return new Result(assignments, componentSizes.toIntArray(), exact, iterative);
    }

    /** connected component of the fragment graph, as {@code AdductNetwork.spread} does it for adduct nodes */
    private static IntArrayList spread(int seed, List<IntArrayList> neighbours, BitSet visited) {
        final IntArrayList stack = new IntArrayList();
        stack.add(seed);
        visited.set(seed);
        for (int i = 0; i < stack.size(); ++i) {
            for (int neighbour : neighbours.get(stack.getInt(i))) {
                if (!visited.get(neighbour)) {
                    visited.set(neighbour);
                    stack.add(neighbour);
                }
            }
        }
        return stack;
    }

    private static void solveComponent(IntArrayList component, List<List<WeightedEdge>> candidates,
                                       List<FragmentEdge> fragmentEdges,
                                       PrecursorCapacity capacity, double unassignedWeight, double minProbability,
                                       int exactSolverLimit, int iterativeSteps, FragmentAssignment[] assignments) {
        // local indices, so the walk only ever sees the nodes of this component
        final Int2IntOpenHashMap fragmentToLocal = new Int2IntOpenHashMap(component.size());
        for (int local = 0; local < component.size(); ++local) fragmentToLocal.put(component.getInt(local), local);
        final Int2IntOpenHashMap precursorToLocal = new Int2IntOpenHashMap();
        precursorToLocal.defaultReturnValue(-1);
        final IntArrayList localToPrecursor = new IntArrayList();
        for (int fragment : component) {
            for (WeightedEdge edge : candidates.get(fragment)) {
                if (edge.weight() <= 0) continue;
                if (!precursorToLocal.containsKey(edge.precursorIndex())) {
                    precursorToLocal.put(edge.precursorIndex(), localToPrecursor.size());
                    localToPrecursor.add(edge.precursorIndex());
                }
            }
        }
        final int unassignedNode = localToPrecursor.size();
        final AbsorbingWalk.Builder builder = AbsorbingWalk.builder(component.size(), unassignedNode + 1);
        for (int local = 0; local < component.size(); ++local) {
            final int fragment = component.getInt(local);
            // capacities are normalised within this fragment's own candidate set, so the best supported candidate
            // keeps its full weight and the unassigned node stays comparable. Only the ratios between candidates
            // matter, and those are preserved.
            double largestCapacity = 0d;
            for (WeightedEdge edge : candidates.get(fragment)) {
                if (edge.weight() <= 0) continue;
                largestCapacity = Math.max(largestCapacity, capacity.capacityOf(edge.precursorIndex()));
            }
            for (WeightedEdge edge : candidates.get(fragment)) {
                final double weight = edge.weight();
                if (weight <= 0) continue;
                final double share = largestCapacity > 0
                        ? Math.max(0d, capacity.capacityOf(edge.precursorIndex())) / largestCapacity
                        : 1d;
                builder.addAbsorbingEdge(local, precursorToLocal.get(edge.precursorIndex()), weight * share);
            }
            builder.addAbsorbingEdge(local, unassignedNode, unassignedWeight);
        }
        for (FragmentEdge edge : fragmentEdges) {
            if (edge.weight() <= 0) continue;
            if (!fragmentToLocal.containsKey(edge.leftFragment()) || !fragmentToLocal.containsKey(edge.rightFragment())) continue;
            builder.addTransientEdge(fragmentToLocal.get(edge.leftFragment()), fragmentToLocal.get(edge.rightFragment()),
                    edge.weight());
        }
        final AbsorbingWalk walk = builder.build();
        final double[][] absorption = component.size() > exactSolverLimit
                ? walk.solveIterative(iterativeSteps)
                : walk.solveExact();

        final int[] precursorIndices = localToPrecursor.toIntArray();
        for (int local = 0; local < component.size(); ++local) {
            final double[] row = absorption[local];
            final double[] probabilities = new double[precursorIndices.length];
            System.arraycopy(row, 0, probabilities, 0, precursorIndices.length);
            assignments[component.getInt(local)] = FragmentAssignment.of(precursorIndices, probabilities,
                    row[unassignedNode], minProbability);
        }
    }
}
