package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resolving candidate edges into per fragment probabilities. The weighting is injected, so these tests state the
 * evidence directly instead of going through correlations and calibration.
 */
public class DiaFragmentResolverTest {

    private static final double MS1_STEP = 0.2;
    private static final double CHANNEL_STEP = 2.24;

    private static ScanPointMapping axis(double step, int length) {
        final double[] rts = new double[length];
        final int[] ids = new int[length];
        for (int k = 0; k < length; ++k) {
            rts[k] = k * step;
            ids[k] = k;
        }
        return new ScanPointMapping(rts, ids, null);
    }

    private static final ScanPointMapping MS1 = axis(MS1_STEP, 1000);
    private static final ScanPointMapping CHANNEL_AXIS = axis(CHANNEL_STEP, 90);

    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_AXIS.length()];
        for (int k = 0; k < toMs1.length; ++k) toMs1[k] = (int) Math.round(CHANNEL_AXIS.getRetentionTimeAt(k) / MS1_STEP);
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), CHANNEL_AXIS, toMs1);
    }

    private static ContiguousTrace precursorTrace(double mz, double centreRt) {
        final double[] masses = new double[MS1.length()];
        final float[] intensities = new float[MS1.length()];
        for (int k = 0; k < MS1.length(); ++k) {
            masses[k] = mz;
            intensities[k] = (float) (10000d * Math.exp(-0.5 * Math.pow((MS1.getRetentionTimeAt(k) - centreRt) / 4d, 2)));
        }
        return new ContiguousTrace(MS1, 0, MS1.length() - 1, masses, intensities);
    }

    private static ContiguousTrace fragmentTrace(double mz, double centreRt) {
        final int from = CHANNEL_AXIS.idForRetentionTime(centreRt - 12);
        final int to = CHANNEL_AXIS.idForRetentionTime(centreRt + 12);
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[masses.length];
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = (float) (2000d * Math.exp(-0.5 * Math.pow((CHANNEL_AXIS.getRetentionTimeAt(k) - centreRt) / 4d, 2)));
        }
        return new ContiguousTrace(CHANNEL_AXIS, from, to, masses, intensities);
    }

    private static PrecursorFeature feature(ContiguousTrace trace, double centreRt) {
        return PrecursorFeature.of(channel(), trace, new TraceSegment(
                (int) Math.round(centreRt / MS1_STEP),
                (int) Math.round((centreRt - 10) / MS1_STEP),
                (int) Math.round((centreRt + 10) / MS1_STEP))).orElseThrow();
    }

    /** every edge is worth the same, so only the graph structure matters */
    private static final DiaFragmentResolver.EdgeWeighting UNIFORM = edge -> 10d;

    @Test
    public void aFragmentWithOneStrongCandidateIsAssignedToIt() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d)), List.of(fragmentTrace(150.1, 60d)));
        assertEquals(1, graph.getPrecursorEdges().size(), "precondition");

        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, UNIFORM);

        FragmentAssignment assignment = result.getAssignments()[0];
        assertEquals(1, assignment.size());
        assertEquals(0, assignment.precursorIndexAt(0));
        // weight 10 against an unassigned weight of 1
        assertEquals(10d / 11d, assignment.probabilityAt(0), 1e-9);
        assertEquals(1d / 11d, assignment.getUnassignedProbability(), 1e-9);
        assertFalse(assignment.isAmbiguous());
        assertEquals(1, result.assignedFragments());
    }

    /** Two precursors, equal evidence: the probability is split rather than the fragment forced onto one. */
    @Test
    public void anAmbiguousFragmentIsSplitBetweenBothPrecursors() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d), feature(precursorTrace(420.7, 61d), 61d)),
                List.of(fragmentTrace(150.1, 60.5d)));

        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, UNIFORM);
        FragmentAssignment assignment = result.getAssignments()[0];

        assertTrue(assignment.isAmbiguous());
        assertEquals(2, assignment.size());
        assertEquals(10d / 21d, assignment.probabilityAt(0), 1e-9);
        assertEquals(10d / 21d, assignment.probabilityAt(1), 1e-9);
        assertEquals(1, result.ambiguousFragments());
    }

    @Test
    public void strongerEvidenceWinsTheLargerShare() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d), feature(precursorTrace(420.7, 61d), 61d)),
                List.of(fragmentTrace(150.1, 60.5d)));
        // the first precursor gets three times the weight
        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph,
                edge -> edge.precursorIndex() == 0 ? 30d : 10d);

        FragmentAssignment assignment = result.getAssignments()[0];
        assertEquals(0, assignment.precursorIndexAt(0), "the better supported precursor comes first");
        assertEquals(30d / 41d, assignment.probabilityAt(0), 1e-9);
        assertEquals(10d / 41d, assignment.probabilityAt(1), 1e-9);
    }

    /** Weak evidence loses to the unassigned node, which is what stops noise being assigned with confidence. */
    @Test
    public void aFragmentWithoutEvidenceStaysUnassigned() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d)), List.of(fragmentTrace(150.1, 60d)));

        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, edge -> 0d);
        FragmentAssignment assignment = result.getAssignments()[0];

        assertTrue(assignment.isEmpty(), "no evidence must mean no stored precursor");
        assertEquals(1d, assignment.getUnassignedProbability(), 1e-9);
        assertTrue(assignment.likelyBelongsToNothing());
        assertEquals(0, result.assignedFragments());
    }

    @Test
    public void negligibleCandidatesAreDropped() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d), feature(precursorTrace(420.7, 61d), 61d)),
                List.of(fragmentTrace(150.1, 60.5d)));
        // the second precursor is worth a thousandth of the first
        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph,
                edge -> edge.precursorIndex() == 0 ? 1000d : 0.5d, List.of(),
                DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT, 0.01, 250, 12);

        FragmentAssignment assignment = result.getAssignments()[0];
        assertEquals(1, assignment.size(), "the negligible precursor must not be stored");
        assertEquals(0, assignment.precursorIndexAt(0));
        assertFalse(assignment.isAmbiguous());
        // the dropped mass is neither renormalised away nor called unassigned
        assertTrue(assignment.storedProbability() + assignment.getUnassignedProbability() < 1d);
    }

    /**
     * Ion count is conserved, so a precursor a hundred times more intense can account for a hundred times more MS2
     * signal. Given equally good elution profiles, that has to decide the assignment - which a threshold on
     * intensity could not do, since it would only remove candidates.
     */
    @Test
    public void anIntensePrecursorOutweighsAWeakOneWithTheSameEvidence() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d), feature(precursorTrace(420.7, 60d), 60d)),
                List.of(fragmentTrace(150.1, 60d)));
        assertEquals(2, graph.edgesTo(0).size(), "precondition: both are candidates");

        // identical evidence, but the first precursor is a hundred times more intense
        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, UNIFORM,
                precursorIndex -> precursorIndex == 0 ? 1_000_000d : 10_000d, List.of(),
                DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT, 0.01, 250, 12);

        FragmentAssignment assignment = result.getAssignments()[0];
        assertEquals(0, assignment.precursorIndexAt(0), "the intense precursor has to come first");
        // weights are 10 and 10/100 against an unassigned weight of 1
        assertEquals(10d / 11.1, assignment.probabilityAt(0), 1e-6);
        assertEquals(1, assignment.size(), "and the weak one falls below the storage threshold");
        assertFalse(assignment.isAmbiguous());
    }

    /**
     * The other half of the argument: this is a comparison between competitors, not an absolute cut. A weak
     * precursor that is the only candidate for a fragment keeps it.
     */
    @Test
    public void aWeakPrecursorWithoutCompetitionKeepsItsFragment() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d)), List.of(fragmentTrace(150.1, 60d)));

        DiaFragmentResolver.Result weak = DiaFragmentResolver.resolve(graph, UNIFORM,
                precursorIndex -> 1d, List.of(), DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT, 0.01, 250, 12);
        DiaFragmentResolver.Result strong = DiaFragmentResolver.resolve(graph, UNIFORM,
                precursorIndex -> 1_000_000d, List.of(), DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT, 0.01, 250, 12);

        assertEquals(1, weak.getAssignments()[0].size());
        assertEquals(strong.getAssignments()[0].probabilityAt(0), weak.getAssignments()[0].probabilityAt(0), 1e-9,
                "with a single candidate the absolute intensity must not matter at all");
    }

    /** Fragment edges are the only thing that makes a component bigger than one fragment. */
    @Test
    public void withoutFragmentEdgesEveryComponentIsASingleFragment() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d)),
                List.of(fragmentTrace(150.1, 60d), fragmentTrace(233.4, 60d), fragmentTrace(301.9, 60d)));

        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, UNIFORM);

        assertEquals(3, result.getComponentSizes().length);
        assertEquals(1, result.largestComponent());
        assertEquals(3, result.getComponentsSolvedExactly());
        assertEquals(0, result.getComponentsSolvedIteratively());
    }

    /**
     * A fragment with no candidate of its own, settled through the sibling it is tied to. This is the
     * disambiguation the fragment edges exist for.
     */
    @Test
    public void aFragmentEdgeCarriesEvidenceToAFragmentWithoutItsOwn() {
        ContiguousTrace precursor = precursorTrace(400.2, 60d);
        ContiguousTrace anchored = fragmentTrace(150.1, 60d);
        // far too heavy to be a candidate of this precursor, so it has no edge of its own
        ContiguousTrace orphan = fragmentTrace(900.9, 60d);
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 60d)), List.of(anchored, orphan));
        assertEquals(1, graph.getPrecursorEdges().size(), "precondition: only the light fragment is a candidate");

        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, UNIFORM,
                List.of(new DiaFragmentResolver.FragmentEdge(0, 1, 20d)),
                DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT, 0.01, 250, 12);

        assertEquals(1, result.getComponentSizes().length, "the fragment edge joins them into one component");
        assertEquals(2, result.largestComponent());
        FragmentAssignment orphanAssignment = result.getAssignments()[1];
        assertEquals(1, orphanAssignment.size(), "it inherits the precursor through its sibling");
        assertEquals(0, orphanAssignment.precursorIndexAt(0));
        assertTrue(orphanAssignment.probabilityAt(0) > 0.5, "got " + orphanAssignment.probabilityAt(0));
        // and it must remain less certain than the fragment that has direct evidence
        assertTrue(orphanAssignment.probabilityAt(0) < result.getAssignments()[0].probabilityAt(0));
    }

    @Test
    public void aLargeComponentIsSolvedIteratively() {
        final int n = 6;
        ContiguousTrace precursor = precursorTrace(400.2, 60d);
        List<ContiguousTrace> fragments = new java.util.ArrayList<>();
        for (int k = 0; k < n; ++k) fragments.add(fragmentTrace(150.1 + k, 60d));
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(), List.of(feature(precursor, 60d)), fragments);

        List<DiaFragmentResolver.FragmentEdge> chain = new java.util.ArrayList<>();
        for (int k = 0; k + 1 < n; ++k) chain.add(new DiaFragmentResolver.FragmentEdge(k, k + 1, 5d));

        // a limit of two forces the iterative solver for this component of six
        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, UNIFORM, chain,
                DiaFragmentResolver.DEFAULT_UNASSIGNED_WEIGHT, 0.01, 2, 40);

        assertEquals(1, result.getComponentSizes().length);
        assertEquals(n, result.largestComponent());
        assertEquals(0, result.getComponentsSolvedExactly());
        assertEquals(1, result.getComponentsSolvedIteratively());
        for (FragmentAssignment a : result.getAssignments()) {
            assertTrue(a.storedProbability() + a.getUnassignedProbability() <= 1d + 1e-9);
        }
    }

    @Test
    public void aGraphWithoutFragmentsResolvesToNothing() {
        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorTrace(400.2, 60d), 60d)), List.of());
        DiaFragmentResolver.Result result = DiaFragmentResolver.resolve(graph, UNIFORM);
        assertEquals(0, result.getAssignments().length);
        assertEquals(0, result.getComponentSizes().length);
    }

    @Test
    public void theWeightTransformSendsNoEvidenceToNoWeight() {
        final double floor = DiaFragmentResolver.DEFAULT_SIGNIFICANCE_FLOOR;
        assertEquals(0d, DiaFragmentResolver.weightFromLogPValue(0d, floor), 1e-12, "no evidence, no weight");
        assertEquals(0d, DiaFragmentResolver.weightFromLogPValue(-floor, floor), 1e-12, "exactly at the floor");
        assertEquals(2d, DiaFragmentResolver.weightFromLogPValue(-floor - 2d, floor), 1e-12);
        assertTrue(DiaFragmentResolver.weightFromLogPValue(-20d, floor)
                > DiaFragmentResolver.weightFromLogPValue(-10d, floor), "more significant means more weight");
        assertEquals(0d, DiaFragmentResolver.weightFromLogPValue(Double.NaN, floor), 1e-12);
    }
}
