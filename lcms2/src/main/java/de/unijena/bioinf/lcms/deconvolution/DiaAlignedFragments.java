package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The same fragment mass, measured in several samples, recognised as one thing.
 * <p>
 * This is what makes stage two more than a repetition of stage one over better windows. A DIA channel samples a peak
 * four to nine times, which is not enough to tell two co-eluting precursors apart; three replicates of that
 * measurement are three independent observations of the same question. Pooling them requires knowing which fragment
 * of sample two is the fragment of sample one, which is what this establishes - the equivalent of what alignment
 * does for MS1 traces, but restricted to one channel and therefore much simpler.
 * <p>
 * Grouping is by mass first, then by apex retention time, and both are deliberately conservative:
 * <ul>
 *     <li>Masses are grouped by <em>gaps</em>, a new group starting where the distance to the previous mass exceeds
 *     what the alignment expects between samples. In a hundred Dalton wide channel the masses of distinct fragments
 *     are typically orders of magnitude further apart than that, so chaining is not a practical concern; where it
 *     does happen the group simply spans a slightly wider mass range.</li>
 *     <li>Retention times are compared as <em>apexes</em>, not as overlapping intervals, and are recalibrated onto
 *     the merged axis first. Overlap chains through anything that elutes broadly, apexes do not.</li>
 * </ul>
 * A sample never appears twice in one group: if the next trace comes from a sample the group already has, it starts
 * a new group even when it is close enough in time. Two traces of one sample at the same mass are two elutions of
 * that fragment, and merging them would invent a peak that was never measured as one.
 * <p>
 * Fragments seen in a single sample are kept. They carry less evidence, not none - the shape correlation over that
 * sample's window still applies - and dropping them would systematically lose everything near the detection limit.
 */
public final class DiaAlignedFragments {

    /** the traces of one channel in one sample, as they come out of the fragment trace store */
    public record SampleFragments(ProcessedSample sample, DiaChannel channel, List<ContiguousTrace> traces) {
    }

    /** one fragment mass in one sample */
    @Getter
    public static final class Instance {
        /** index into the samples given to {@link #group}, not the sample's own uid */
        private final int sampleIndex;
        private final ContiguousTrace trace;
        /** apex retention time, recalibrated onto the merged axis so that samples are comparable */
        private final double apexRetentionTime;

        private Instance(int sampleIndex, ContiguousTrace trace, double apexRetentionTime) {
            this.sampleIndex = sampleIndex;
            this.trace = trace;
            this.apexRetentionTime = apexRetentionTime;
        }

        double mz() {
            return trace.averagedMz();
        }
    }

    /** one fragment, across the samples that measured it */
    @Getter
    public static final class Aligned {
        private final double mz;
        private final double apexRetentionTime;
        private final List<Instance> instances;

        private Aligned(double mz, double apexRetentionTime, List<Instance> instances) {
            this.mz = mz;
            this.apexRetentionTime = apexRetentionTime;
            this.instances = instances;
        }

        /** @return the trace of this fragment in the given sample, or null if that sample did not measure it */
        public ContiguousTrace traceIn(int sampleIndex) {
            for (Instance instance : instances) {
                if (instance.sampleIndex == sampleIndex) return instance.trace;
            }
            return null;
        }

        public int numberOfSamples() {
            return instances.size();
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "AlignedFragment(m/z %.4f, rt %.1f s, %d samples)",
                    mz, apexRetentionTime, instances.size());
        }
    }

    private DiaAlignedFragments() {
    }

    /**
     * @param perSample     the same channel in every sample; a sample that does not have this channel is left out
     * @param massDeviation how far the same mass may differ between samples, i.e.
     *                      {@code AlignmentStatistics.getExpectedMassDeviationBetweenSamples}
     * @param rtTolerance   how far the same apex may differ between samples after recalibration, in seconds
     */
    public static List<Aligned> group(List<SampleFragments> perSample, Deviation massDeviation, double rtTolerance) {
        final List<Instance> instances = new ArrayList<>();
        for (int s = 0; s < perSample.size(); ++s) {
            final SampleFragments fragments = perSample.get(s);
            for (ContiguousTrace trace : fragments.traces()) {
                final double apexRt = fragments.sample().getRtRecalibration()
                        .value(trace.retentionTime(trace.apex()));
                instances.add(new Instance(s, trace, apexRt));
            }
        }
        instances.sort(Comparator.comparingDouble(Instance::mz));

        final List<Aligned> aligned = new ArrayList<>();
        int from = 0;
        for (int k = 1; k <= instances.size(); ++k) {
            final boolean gap = k == instances.size() || instances.get(k).mz() - instances.get(k - 1).mz()
                    > massDeviation.absoluteFor(instances.get(k).mz());
            if (!gap) continue;
            groupByRetentionTime(instances.subList(from, k), rtTolerance, aligned);
            from = k;
        }
        return aligned;
    }

    private static void groupByRetentionTime(List<Instance> sameMass, double rtTolerance, List<Aligned> out) {
        final List<Instance> byTime = new ArrayList<>(sameMass);
        byTime.sort(Comparator.comparingDouble(Instance::getApexRetentionTime));
        List<Instance> current = new ArrayList<>();
        for (Instance instance : byTime) {
            if (!current.isEmpty() && (instance.apexRetentionTime
                    - current.get(current.size() - 1).apexRetentionTime > rtTolerance
                    || containsSample(current, instance.sampleIndex))) {
                out.add(finish(current));
                current = new ArrayList<>();
            }
            current.add(instance);
        }
        if (!current.isEmpty()) out.add(finish(current));
    }

    private static boolean containsSample(List<Instance> instances, int sampleIndex) {
        for (Instance instance : instances) {
            if (instance.sampleIndex == sampleIndex) return true;
        }
        return false;
    }

    private static Aligned finish(List<Instance> instances) {
        double mz = 0d, rt = 0d;
        for (Instance instance : instances) {
            mz += instance.mz();
            rt += instance.apexRetentionTime;
        }
        return new Aligned(mz / instances.size(), rt / instances.size(), List.copyOf(instances));
    }
}
