package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.statistics.TraceCorrelationStatistics;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Grouping the precursors a channel cannot separate. The chaining test is the one that matters: pairwise similarity
 * must not accumulate into a group whose ends have nothing to do with each other.
 */
public class DiaPrecursorGroupsTest {

    private static final double MS1_STEP = 0.2;
    private static final double CHANNEL_STEP = 2.24;
    private static final int CHANNEL_LENGTH = 90;

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

    private static DiaChannel channel() {
        final int[] toMs1 = new int[CHANNEL_LENGTH];
        for (int k = 0; k < CHANNEL_LENGTH; ++k) {
            toMs1[k] = Math.min(MS1.length() - 1, (int) Math.round(k * CHANNEL_STEP / MS1_STEP));
        }
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35),
                axis(CHANNEL_STEP, CHANNEL_LENGTH), toMs1);
    }

    /** an MS1 trace peaking at the given time, with the given apex height and width in seconds */
    private static ContiguousTrace trace(double mz, double centre, double apex, double sigma) {
        final double[] masses = new double[MS1.length()];
        final float[] intensities = new float[MS1.length()];
        for (int k = 0; k < masses.length; ++k) {
            masses[k] = mz;
            intensities[k] = (float) (apex * Math.exp(-0.5 * Math.pow((MS1.getRetentionTimeAt(k) - centre) / sigma, 2)));
        }
        return new ContiguousTrace(MS1, 0, masses.length - 1, masses, intensities);
    }

    /** the feature of that trace, over a window of about twenty seconds around its apex */
    private static PrecursorFeature feature(ContiguousTrace trace, double centre) {
        final TraceSegment segment = new TraceSegment((int) Math.round(centre / MS1_STEP),
                (int) Math.round((centre - 10) / MS1_STEP), (int) Math.round((centre + 10) / MS1_STEP));
        return PrecursorFeature.of(channel(), trace, segment).orElseThrow();
    }

    private static PrecursorFeature at(double mz, double centre, double apex) {
        return feature(trace(mz, centre, apex, 3d), centre);
    }

    @Test
    public void twoPrecursorsThatCoEluteExactlyAreOneGroup() {
        final List<PrecursorFeature> features = List.of(at(400.2, 60d, 100_000d), at(420.7, 60d, 1_000_000d));
        final List<DiaPrecursorGroups.Group> groups = DiaPrecursorGroups.of(channel(), features);

        assertEquals(1, groups.size());
        final DiaPrecursorGroups.Group group = groups.get(0);
        assertEquals(2, group.size());
        assertEquals(1, group.getRepresentative(), "the more intense one represents the group");
        assertEquals(1, group.getMembers()[0], "and comes first");
        assertEquals(100_000d / 1_100_000d, group.getPollutionRatio(), 1e-6,
                "the weaker one is the pollution");
    }

    @Test
    public void twoPrecursorsElutingApartStayApart() {
        final List<PrecursorFeature> features = List.of(at(400.2, 30d, 500_000d), at(420.7, 70d, 500_000d));
        final List<DiaPrecursorGroups.Group> groups = DiaPrecursorGroups.of(channel(), features);

        assertEquals(2, groups.size());
        for (DiaPrecursorGroups.Group group : groups) {
            assertEquals(1, group.size());
            assertFalse(group.isAmbiguous());
            assertEquals(0d, group.getPollutionRatio(), 0d, "a group of one is not polluted");
        }
    }

    @Test
    public void aChainOfSimilarPrecursorsDoesNotBecomeOneGroup() {
        // A and B are similar, B and C are similar, A and C are not. Complete linkage has to refuse the chain,
        // which is the trap that made retention time overlap useless as a criterion.
        // one second apart correlates at about 0.95 at this channel's resolution, two seconds at about 0.82
        final PrecursorFeature a = at(400.2, 59d, 900_000d);
        final PrecursorFeature b = at(420.7, 60d, 1_000_000d);
        final PrecursorFeature c = at(440.9, 61d, 800_000d);
        assertTrue(DiaPrecursorGroups.correlate(channel(), a, b).correlation() > 0.9, "precondition: A and B similar");
        assertTrue(DiaPrecursorGroups.correlate(channel(), b, c).correlation() > 0.9, "precondition: B and C similar");
        assertTrue(DiaPrecursorGroups.correlate(channel(), a, c).correlation() < 0.9, "precondition: A and C are not");

        final List<DiaPrecursorGroups.Group> groups =
                DiaPrecursorGroups.of(channel(), List.of(a, b, c), 0.9, DiaPrecursorGroups.DEFAULT_MAX_APEX_DISTANCE);

        for (DiaPrecursorGroups.Group group : groups) {
            assertTrue(group.size() <= 2, "no group may hold all three: " + group);
        }
        // and every feature is in exactly one group
        assertEquals(3, groups.stream().mapToInt(DiaPrecursorGroups.Group::size).sum());
    }

    @Test
    public void everyFeatureEndsUpInExactlyOneGroup() {
        final List<PrecursorFeature> features = new ArrayList<>();
        for (int k = 0; k < 12; ++k) features.add(at(400.2 + k, 20d + 5d * k, 100_000d + 1000d * k));
        final List<DiaPrecursorGroups.Group> groups = DiaPrecursorGroups.of(channel(), features);

        final boolean[] seen = new boolean[features.size()];
        for (DiaPrecursorGroups.Group group : groups) {
            for (int member : group.getMembers()) {
                assertFalse(seen[member], "feature " + member + " is in two groups");
                seen[member] = true;
            }
        }
        for (int k = 0; k < seen.length; ++k) assertTrue(seen[k], "feature " + k + " is in no group");
    }

    @Test
    public void groupingIsDeterministicWhateverTheInputOrder() {
        final PrecursorFeature a = at(400.2, 60d, 100_000d);
        final PrecursorFeature b = at(420.7, 60d, 1_000_000d);
        final List<DiaPrecursorGroups.Group> one = DiaPrecursorGroups.of(channel(), List.of(a, b));
        final List<DiaPrecursorGroups.Group> other = DiaPrecursorGroups.of(channel(), List.of(b, a));

        assertEquals(1, one.size());
        assertEquals(1, other.size());
        // the same feature represents the group either way; only its index differs with the input order
        assertEquals(1, one.get(0).getRepresentative());
        assertEquals(0, other.get(0).getRepresentative());
        assertEquals(one.get(0).getPollutionRatio(), other.get(0).getPollutionRatio(), 1e-12);
    }

    @Test
    public void precursorsTooFarApartInApexAreNeverCompared() {
        // identical shapes, but far enough apart that no fragment could be a candidate of both
        final int distance = DiaPrecursorGroups.DEFAULT_MAX_APEX_DISTANCE + 2;
        final double centre = 60d, other = centre + distance * CHANNEL_STEP;
        final List<DiaPrecursorGroups.Group> groups = DiaPrecursorGroups.of(channel(),
                List.of(at(400.2, centre, 500_000d), at(420.7, other, 500_000d)));
        assertEquals(2, groups.size(), "beyond the apex distance there is nothing to be indistinguishable about");
    }

    @Test
    public void aSharpAndABroadPeakAreNotTheSameElution() {
        // same apex, very different width: the union of the windows is where that shows
        final PrecursorFeature sharp = feature(trace(400.2, 60d, 500_000d, 1.5), 60d);
        final PrecursorFeature broad = feature(trace(420.7, 60d, 500_000d, 8d), 60d);
        assertTrue(DiaPrecursorGroups.correlate(channel(), sharp, broad).correlation()
                < DiaPrecursorGroups.DEFAULT_MIN_CORRELATION, "precondition: distinguishable");

        assertEquals(2, DiaPrecursorGroups.of(channel(), List.of(sharp, broad)).size());
    }

    /**
     * A measured co-elution standard is followed, down to the floor of the statistic that measured it.
     * <p>
     * Grouping imposes no minimum of its own: the bar lives in {@link TraceCorrelationStatistics#MINIMUM_CUTOFF},
     * which is the only thing {@code cutoffFor} consults, and raising it there is how the standard is tightened.
     * <p>
     * This comment used to carry two figures - that raising the few-point cutoff from 0.8 to 0.9 cost seven library
     * identifications and the many-point one from 0.9 to 0.95 cost five. Neither reproduces. Re-measured 2026-08-21
     * on the same eighteen samples, after the determinism and isotope fixes, raising the few-point cutoff alone
     * *gained* one and raising the many-point one alone cost four; the spread over five settings of the pair is six,
     * not monotone, and no larger than the disagreement between configurations that score the same. The figures were
     * taken before the isotope fix removed 489628 non-finite m/z values and moved the feature count by a hundred, so
     * they described a pipeline that no longer exists.
     */
    @Test
    public void aMeasuredStandardIsFollowedDownToTheStatisticsOwnFloor() {
        // 0.7 s apart, which clears the many-point floor. The pair used to be one second apart, correlating at
        // about 0.947: that sat above the floor when it was 0.9 and below it once it became 0.95, so the fixture
        // moved with the constant. What is being tested is unchanged - a learned standard below the floor is
        // raised to the floor and no further, and a pair that clears the floor is grouped.
        final List<PrecursorFeature> features = List.of(at(400.2, 59.3d, 900_000d), at(420.7, 60d, 1_000_000d));
        final TraceCorrelationStatistics lenient = statisticsWith(0.6, 0d);
        final double cutoff = lenient.cutoffFor(20, 1e6, 100d, 0.05);
        assertEquals(TraceCorrelationStatistics.minimumCutoff(20), cutoff, 1e-9,
                "precondition: a measurement below the floor is raised to it, and no further");

        assertEquals(1, DiaPrecursorGroups.of(channel(), features, lenient, 100d).groups().size(),
                "the pair clears the floor, so nothing above it may keep them apart");
    }

    /** a distribution of known same-elutions, as the sample statistics would have learned it */
    private static TraceCorrelationStatistics statisticsWith(double minCorrelationOfSameElution,
                                                             double minUsableMultipleOfNoise) {
        final float[][] bins = new float[TraceCorrelationStatistics.binCount()][];
        java.util.Arrays.fill(bins, new float[0]);
        for (int points = 0; points < 2; ++points) {
            for (int intensity = 0; intensity < TraceCorrelationStatistics.INTENSITY_BINS; ++intensity) {
                final double lower = intensity == 0 ? 0d : TraceCorrelationStatistics.INTENSITY_MULTIPLES[intensity - 1];
                final float[] values = new float[TraceCorrelationStatistics.MIN_OBSERVATIONS * 2];
                // below the usable intensity the pairs do not correlate, which is what makes the bin unusable
                java.util.Arrays.fill(values, lower < minUsableMultipleOfNoise ? 0.1f
                        : (float) minCorrelationOfSameElution);
                bins[points * TraceCorrelationStatistics.INTENSITY_BINS + intensity] = values;
            }
        }
        return new TraceCorrelationStatistics(bins);
    }

    @Test
    public void theThresholdComesFromWhatARealCoElutionReaches() {
        // 0.4 s apart correlates at 0.9916 here. Both standards below are above DEFAULT_MIN_CORRELATION on purpose:
        // this test is about the learned distribution deciding, and a standard under the floor would be decided by
        // the floor instead - which is what aLenientMeasurementDoesNotLowerWhatGroupingDemands covers.
        final List<PrecursorFeature> features = List.of(at(400.2, 59.6, 900_000d), at(420.7, 60d, 1_000_000d));
        final double noise = 100d;

        assertEquals(2, DiaPrecursorGroups.of(channel(), features, statisticsWith(0.999, 0d), noise)
                .groups().size(), "a strict co-elution standard tells them apart");
        assertEquals(1, DiaPrecursorGroups.of(channel(), features, statisticsWith(0.96, 0d), noise)
                .groups().size(), "a lenient one cannot");
    }

    @Test
    public void nothingToGroupIsNoProblem() {
        assertTrue(DiaPrecursorGroups.of(channel(), List.of()).isEmpty());
        assertEquals(1, DiaPrecursorGroups.of(channel(), List.of(at(400.2, 60d, 100d))).size());
    }

    /**
     * A group speaks for more than its representative when a member is nearly as intense.
     * <p>
     * Giving the group's fragments only to its most intense member is what a data dependent acquisition does, and it
     * is honest where one member dominates. Where two members are of comparable abundance it is a coin toss, and the
     * evidence for calling one of them the owner does not exist - so both are reported, each carrying the group's
     * pollution ratio. The threshold is on the share of the dominant member's intensity, not on an absolute value,
     * because what makes the choice arbitrary is the ratio.
     */
    @Test
    public void aGroupReportsEveryMemberComparableToItsDominant() {
        final List<PrecursorFeature> features = List.of(at(400.2, 60d, 100_000d), at(420.7, 60d, 60_000d),
                at(430.3, 60d, 10_000d));
        final List<DiaPrecursorGroups.Group> groups = DiaPrecursorGroups.of(channel(), features, 0.9,
                DiaPrecursorGroups.DEFAULT_MAX_APEX_DISTANCE);
        assertEquals(1, groups.size(), "three identical elutions are one group");

        final int[] half = groups.getFirst().reportedMembers(features, 0.5);
        assertEquals(2, half.length, "the dominant and the member at 60 % of it");
        assertEquals(groups.getFirst().getRepresentative(), half[0], "the representative comes first");

        assertEquals(1, groups.getFirst().reportedMembers(features, 1.0).length,
                "at a threshold of one only the dominant speaks");
        assertEquals(3, groups.getFirst().reportedMembers(features, 0.05).length,
                "and a low enough threshold reports everything, which is the ungrouped behaviour");
    }

    /** a group of one has nothing to decide */
    @Test
    public void aGroupOfOneReportsItself() {
        final List<PrecursorFeature> features = List.of(at(400.2, 60d, 100_000d));
        final List<DiaPrecursorGroups.Group> groups = DiaPrecursorGroups.of(channel(), features,
                DiaPrecursorGroups.DEFAULT_MIN_CORRELATION, DiaPrecursorGroups.DEFAULT_MAX_APEX_DISTANCE);
        assertArrayEquals(new int[]{0}, groups.getFirst().reportedMembers(features, 0.5));
    }
}
