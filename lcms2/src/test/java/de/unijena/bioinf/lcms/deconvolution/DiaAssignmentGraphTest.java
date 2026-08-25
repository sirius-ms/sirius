package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Candidate edges between precursor features and fragments of one channel. The graph decides only what could
 * belong together, and records how strong the evidence is - never what a fragment finally belongs to.
 */
public class DiaAssignmentGraphTest {

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

    private static ContiguousTrace peaks(ScanPointMapping ax, double mz, double baseline, double[]... centreSigmaApex) {
        final double[] masses = new double[ax.length()];
        final float[] intensities = new float[ax.length()];
        for (int k = 0; k < ax.length(); ++k) {
            double v = baseline;
            for (double[] p : centreSigmaApex) v += p[2] * Math.exp(-0.5 * Math.pow((ax.getRetentionTimeAt(k) - p[0]) / p[1], 2));
            masses[k] = mz;
            intensities[k] = (float) v;
        }
        return new ContiguousTrace(ax, 0, ax.length() - 1, masses, intensities);
    }

    /** a fragment measured only around centreRt, as a well behaved fragment of a single compound would be */
    private static ContiguousTrace localFragment(double mz, double centreRt, double sigma, float apex) {
        final int from = CHANNEL_AXIS.idForRetentionTime(centreRt - 3 * sigma);
        final int to = CHANNEL_AXIS.idForRetentionTime(centreRt + 3 * sigma);
        final double[] masses = new double[to - from + 1];
        final float[] intensities = new float[masses.length];
        for (int k = from; k <= to; ++k) {
            masses[k - from] = mz;
            intensities[k - from] = (float) (apex * Math.exp(-0.5 * Math.pow((CHANNEL_AXIS.getRetentionTimeAt(k) - centreRt) / sigma, 2)));
        }
        return new ContiguousTrace(CHANNEL_AXIS, from, to, masses, intensities);
    }

    private static TraceSegment segment(double startRt, double apexRt, double endRt) {
        return new TraceSegment((int) Math.round(apexRt / MS1_STEP), (int) Math.round(startRt / MS1_STEP),
                (int) Math.round(endRt / MS1_STEP));
    }

    private static PrecursorFeature feature(ContiguousTrace trace, double startRt, double apexRt, double endRt) {
        Optional<PrecursorFeature> f = PrecursorFeature.of(channel(), trace, segment(startRt, apexRt, endRt));
        assertTrue(f.isPresent(), "the fixture must produce a usable precursor feature");
        return f.get();
    }

    @Test
    public void aCoElutingFragmentBecomesAnEdgeWithStrongEvidence() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace fragment = localFragment(150.1, 60d, 4d, 2000f);

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(fragment));

        assertEquals(1, graph.getPrecursorEdges().size());
        DiaAssignmentGraph.PrecursorEdge edge = graph.getPrecursorEdges().getFirst();
        assertTrue(edge.hasShapeEvidence());
        assertTrue(edge.correlation().correlation() > 0.95, "got " + edge.correlation().correlation());
        assertTrue(edge.specificity() > 0.9, "a fragment seen only here is specific, got " + edge.specificity());
        assertEquals(0, graph.getFeaturesWithoutShapeEvidence());
    }

    @Test
    public void aFragmentElutingElsewhereGetsNoEdge() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace fragment = localFragment(150.1, 140d, 4d, 2000f);

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(fragment));

        assertTrue(graph.getPrecursorEdges().isEmpty());
        assertEquals(1, graph.getFeaturesWithoutShapeEvidence());
    }

    /** A background mass gets an edge, but its specificity says it is present all over the run. */
    @Test
    public void aBackgroundFragmentIsRecordedAsUnspecific() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace background = peaks(CHANNEL_AXIS, 150.1, 500d, new double[]{60d, 4d, 200d});

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(background));

        assertEquals(1, graph.getPrecursorEdges().size());
        assertTrue(graph.getPrecursorEdges().getFirst().specificity() < 0.2,
                "a mass present across the whole run is not specific, got " + graph.getPrecursorEdges().getFirst().specificity());
    }

    /**
     * A fragment the channel caught twice has no elution profile, but the edge is kept: specificity still speaks
     * for it, and after alignment abundance across samples may too.
     */
    @Test
    public void aFragmentWithoutAShapeKeepsItsEdge() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        final int apex = channel().channelIndexForRetentionTime(60d);
        ContiguousTrace fragment = new ContiguousTrace(CHANNEL_AXIS, apex, apex + 1,
                new double[]{150.1, 150.1}, new float[]{800f, 500f});

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(fragment));

        assertEquals(1, graph.getPrecursorEdges().size(), "the edge must survive the missing shape");
        DiaAssignmentGraph.PrecursorEdge edge = graph.getPrecursorEdges().getFirst();
        assertFalse(edge.hasShapeEvidence());
        assertEquals(1d, edge.specificity(), 1e-9, "all of its signal is inside this feature");
        assertEquals(1, graph.getFeaturesWithoutShapeEvidence(), "and it is counted as a feature without a shape");
    }

    @Test
    public void aFragmentHeavierThanItsPrecursorIsNoCandidate() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace tooHeavy = localFragment(700.5, 60d, 4d, 2000f);
        ContiguousTrace justAbove = localFragment(401.0, 60d, 4d, 2000f);   // within the isotope slack

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(tooHeavy, justAbove));

        assertEquals(1, graph.getPrecursorEdges().size());
        assertEquals(1, graph.getPrecursorEdges().getFirst().fragmentIndex(), "only the one within the slack");
    }

    /**
     * The decoy of an all ion channel, where no precursor lies outside the isolation window and the usual decoys
     * cannot be had: the same pair, wrong for one reason only - a fragment cannot be heavier than the precursor it
     * came from. Everything the correlation depends on is untouched, above all that the two peak together.
     */
    @Test
    public void aHeavierFragmentIsTheDecoyOfAnAllIonChannel() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace heavier = localFragment(700.5, 60d, 4d, 2000f);
        ContiguousTrace lighter = localFragment(150.1, 60d, 4d, 2000f);

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(heavier, lighter),
                DiaAssignmentGraph.DEFAULT_APEX_TOLERANCE, DiaAssignmentGraph.DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR,
                DiaAssignmentGraph.MassRelation.HEAVIER_THAN_PRECURSOR);

        assertEquals(1, graph.getPrecursorEdges().size(), "only the heavier one is a decoy edge");
        assertEquals(0, graph.getPrecursorEdges().getFirst().fragmentIndex());
    }

    /**
     * A precursor's own isotopes and adducts are heavier than it and co-elute with it perfectly. Sampled as decoys
     * they would fill the null with correlations of one and make every real edge look unremarkable, so a decoy has
     * to be far enough above the precursor to be another compound.
     */
    @Test
    public void aPrecursorsOwnIsotopesAndAdductsAreNoDecoys() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace secondIsotope = localFragment(402.207, 60d, 4d, 2000f);   // M+2
        ContiguousTrace sodiated = localFragment(422.182, 60d, 4d, 2000f);        // [M+Na]+ against [M+H]+
        ContiguousTrace anotherCompound = localFragment(700.5, 60d, 4d, 2000f);

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)),
                List.of(secondIsotope, sodiated, anotherCompound),
                DiaAssignmentGraph.DEFAULT_APEX_TOLERANCE, DiaAssignmentGraph.DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR,
                DiaAssignmentGraph.MassRelation.HEAVIER_THAN_PRECURSOR);

        assertEquals(1, graph.getPrecursorEdges().size());
        assertEquals(2, graph.getPrecursorEdges().getFirst().fragmentIndex(), "only the unrelated mass");
    }

    /** the decoy still has to look like an edge in every other respect */
    @Test
    public void aHeavierFragmentPeakingElsewhereIsNoDecoyEither() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace heavier = localFragment(700.5, 60d + 4 * CHANNEL_STEP, 3d, 2000f);

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(heavier),
                DiaAssignmentGraph.DEFAULT_APEX_TOLERANCE, DiaAssignmentGraph.DEFAULT_MAX_FRAGMENT_MZ_ABOVE_PRECURSOR,
                DiaAssignmentGraph.MassRelation.HEAVIER_THAN_PRECURSOR);

        assertTrue(graph.getPrecursorEdges().isEmpty(), "a decoy that does not co-elute measures nothing");
    }

    @Test
    public void aFragmentPeakingWellBesideTheFeatureIsNoCandidate() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        // overlaps the window but peaks four scan points away, beyond the apex tolerance
        ContiguousTrace fragment = localFragment(150.1, 60d + 4 * CHANNEL_STEP, 3d, 2000f);

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursor, 50d, 60d, 70d)), List.of(fragment));

        assertTrue(graph.getPrecursorEdges().isEmpty());
    }

    /** An ambiguous fragment gets an edge to every precursor within reach; nothing is decided here. */
    @Test
    public void aFragmentWithinReachOfTwoFeaturesGetsBothEdges() {
        ContiguousTrace precursorA = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        ContiguousTrace precursorB = peaks(MS1, 420.7, 0d, new double[]{61d, 4d, 8000d});
        ContiguousTrace shared = localFragment(150.1, 60.5d, 4d, 2000f);

        DiaAssignmentGraph graph = DiaAssignmentGraph.build(channel(),
                List.of(feature(precursorA, 50d, 60d, 70d), feature(precursorB, 51d, 61d, 71d)), List.of(shared));

        assertEquals(2, graph.edgesTo(0).size(), "both precursors are candidates");
        assertEquals(1, graph.edgesOf(0).size());
        assertEquals(1, graph.edgesOf(1).size());
    }

    @Test
    public void featuresOutsideTheChannelsMassRangeAreNotPrecursors() {
        // the channel isolates 350 to 450, this trace is far below it
        ContiguousTrace elsewhere = peaks(MS1, 120.5, 0d, new double[]{60d, 4d, 10000d});
        assertTrue(PrecursorFeature.of(channel(), elsewhere, segment(50d, 60d, 70d)).isEmpty());
    }

    @Test
    public void tracesWithoutSegmentationContributeNoPrecursors() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{60d, 4d, 10000d});
        assertNull(precursor.getSegments(), "precondition: this fixture has no segmentation");
        assertTrue(PrecursorFeature.of(channel(), List.of(precursor)).isEmpty());
    }

    @Test
    public void everySegmentOfATraceBecomesItsOwnPrecursorFeature() {
        ContiguousTrace precursor = peaks(MS1, 400.2, 0d, new double[]{40d, 4d, 10000d}, new double[]{100d, 4d, 12000d});
        precursor.setSegments(new TraceSegment[]{segment(30d, 40d, 50d), segment(90d, 100d, 110d)});

        List<PrecursorFeature> features = PrecursorFeature.of(channel(), List.of(precursor));

        assertEquals(2, features.size());
        assertEquals(40d, features.get(0).getRetentionTime(), 1d);
        assertEquals(100d, features.get(1).getRetentionTime(), 1d);
        assertFalse(features.get(0).getWindow().overlaps(features.get(1).getWindow()));
    }
}
