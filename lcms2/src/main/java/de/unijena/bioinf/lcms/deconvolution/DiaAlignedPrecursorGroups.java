package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.statistics.TraceCorrelationStatistics;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The counterpart of {@link DiaPrecursorGroups} for stage two: precursors a channel cannot separate, decided over the
 * final feature boundaries and every sample at once.
 * <p>
 * Grouping has to happen at both stages or at neither. Stage two rebuilds the assignment over the final features and
 * rewrites every spectrum stage one wrote, so a grouping applied only to stage one survives in which spectra exist
 * and in nothing else - the peaks would come from an ungrouped competition, which is the very thing the grouping is
 * meant to avoid.
 * <p>
 * A pair is grouped only if it is indistinguishable in <em>every</em> sample that measured both. That is deliberately
 * the strict direction: with three samples a coincidental agreement has to survive three times, and a pair that one
 * sample can separate is separable. Making too many, too small groups costs nothing - in the limit it is the
 * ungrouped assignment - whereas merging two compounds that could have been told apart withholds a spectrum, so the
 * asymmetry belongs in the conservative direction.
 * <p>
 * The cutoff per sample is that sample's own, read from its isotope pairs by
 * {@link TraceCorrelationStatistics}: the samples of one experiment do not have to be equally noisy, and a threshold
 * borrowed from a cleaner run would over-group the dirtier one.
 */
public final class DiaAlignedPrecursorGroups {

    private DiaAlignedPrecursorGroups() {
    }

    /**
     * One set of aligned precursors that no sample can separate.
     *
     * @param members        indices into the precursor list handed in, the representative among them
     * @param representative the most intense member, which the fragments are assigned to
     * @param pollutionRatio share of the group's intensity that is not the representative's
     */
    @Getter
    public static final class Group {
        private final int[] members;
        private final int representative;
        private final double pollutionRatio;

        Group(int[] members, int representative, double pollutionRatio) {
            this.members = members;
            this.representative = representative;
            this.pollutionRatio = pollutionRatio;
        }

        public boolean isAmbiguous() {
            return members.length > 1;
        }

        /** as {@link DiaPrecursorGroups.Group#reportedMembers}, over the aligned features' intensities */
        public int[] reportedMembers(double[] intensities, double minShareOfDominant) {
            double dominant = 0d;
            for (int member : members) dominant = Math.max(dominant, intensities[member]);
            final IntArrayList reported = new IntArrayList(members.length);
            reported.add(representative);
            if (dominant > 0) {
                for (int member : members) {
                    if (member == representative) continue;
                    if (intensities[member] >= minShareOfDominant * dominant) reported.add(member);
                }
            }
            return reported.toIntArray();
        }
    }

    /**
     * @param views        per precursor, its view in each sample, or null where that sample did not measure it, as
     *                     {@code DiaConsensusGraph} computes them
     * @param samples      the samples the views are indexed by
     * @param intensities  per precursor, the apex intensity that decides the representative
     * @param maxApexDistance how far apart two apexes may be, in seconds
     * @param quantile     which quantile of known same elutions the cutoff is read at
     */
    public static List<Group> of(List<DiaFinalPrecursors.Aligned> precursors, PrecursorFeature[][] views,
                                 List<DiaAlignedFragments.SampleFragments> samples, double[] intensities,
                                 double maxApexDistance, double quantile) {
        final int n = precursors.size();
        final List<Group> groups = new ArrayList<>();
        if (n == 0) return groups;

        // most intense first, so a group's seed is its representative and the result does not depend on input order
        final Integer[] byIntensity = new Integer[n];
        for (int k = 0; k < n; ++k) byIntensity[k] = k;
        java.util.Arrays.sort(byIntensity,
                Comparator.comparingDouble((Integer k) -> -intensities[k]).thenComparingInt(k -> k));
        // and by retention time, so only the precursors that could compete are ever compared
        final Integer[] byTime = new Integer[n];
        for (int k = 0; k < n; ++k) byTime[k] = k;
        java.util.Arrays.sort(byTime,
                Comparator.comparingDouble((Integer k) -> precursors.get(k).getRetentionTime()).thenComparingInt(k -> k));
        final int[] timeOrder = new int[n];
        for (int position = 0; position < n; ++position) timeOrder[byTime[position]] = position;

        final boolean[] taken = new boolean[n];
        final IntArrayList members = new IntArrayList();
        for (int seed : byIntensity) {
            if (taken[seed]) continue;
            taken[seed] = true;
            members.clear();
            members.add(seed);
            final int from = timeOrder[seed];
            for (int direction = -1; direction <= 1; direction += 2) {
                for (int position = from + direction; position >= 0 && position < n; position += direction) {
                    final int candidate = byTime[position];
                    if (Math.abs(precursors.get(candidate).getRetentionTime()
                            - precursors.get(seed).getRetentionTime()) > maxApexDistance) break;
                    if (taken[candidate]) continue;
                    if (indistinguishableFromAll(members, candidate, views, samples, quantile)) {
                        taken[candidate] = true;
                        members.add(candidate);
                    }
                }
            }
            groups.add(finish(members, seed, intensities));
        }
        return groups;
    }

    /** complete linkage over the members, and over the samples within each pair */
    private static boolean indistinguishableFromAll(IntArrayList members, int candidate, PrecursorFeature[][] views,
                                                    List<DiaAlignedFragments.SampleFragments> samples,
                                                    double quantile) {
        for (int member : members) {
            if (!indistinguishable(member, candidate, views, samples, quantile)) return false;
        }
        return true;
    }

    /**
     * @return true if every sample that measured both found them indistinguishable, and at least one did measure both
     */
    private static boolean indistinguishable(int left, int right, PrecursorFeature[][] views,
                                             List<DiaAlignedFragments.SampleFragments> samples, double quantile) {
        final PrecursorFeature[] leftViews = views[left], rightViews = views[right];
        if (leftViews == null || rightViews == null) return false;
        boolean judged = false;
        for (int s = 0; s < samples.size(); ++s) {
            final PrecursorFeature a = leftViews[s], b = rightViews[s];
            if (a == null || b == null) continue;
            final DiaChannel channel = samples.get(s).channel();
            final DiaTraceCorrelation.Result correlation = DiaPrecursorGroups.correlate(channel, a, b);
            // a sample that cannot judge the pair does not get a say either way
            if (!correlation.isDefined()) continue;
            judged = true;
            final ProcessedSample sample = samples.get(s).sample();
            final TraceCorrelationStatistics statistics =
                    sample.getStorage().getStatistics().getTraceCorrelations();
            final double noiseLevel = TraceCorrelationStatistics.medianNoiseLevel(sample.getStorage().getStatistics());
            // as in DiaPrecursorGroups: the sample's own standard, floored per datapoint condition and no further
            final double cutoff = statistics.cutoffFor(a.getWindow().union(b.getWindow()).length(),
                    Math.min(a.getApexIntensity(), b.getApexIntensity()), noiseLevel, quantile);
            if (correlation.correlation() < cutoff) return false;
        }
        return judged;
    }

    private static Group finish(IntArrayList members, int representative, double[] intensities) {
        double total = 0d;
        for (int member : members) total += Math.max(0d, intensities[member]);
        final double mine = Math.max(0d, intensities[representative]);
        final double pollution = total > 0d ? (total - mine) / total : 0d;
        final int[] ordered = members.toIntArray();
        for (int k = 0; k < ordered.length; ++k) {
            if (ordered[k] == representative) {
                ordered[k] = ordered[0];
                ordered[0] = representative;
                break;
            }
        }
        return new Group(ordered, representative, pollution);
    }
}
