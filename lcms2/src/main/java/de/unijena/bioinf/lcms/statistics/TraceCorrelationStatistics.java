package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * How well two elution profiles of one and the same compound correlate, measured on this sample.
 * <p>
 * Wherever a correlation decides whether two things co-elute, the question is how high it has to be before it means
 * anything, and over a handful of points that is not obvious. For a fragment against a precursor the question is
 * answered with decoys - pairs that cannot belong together. For two <em>precursors</em> it cannot be: what would make
 * a pair a decoy is that the two are not the same elution, and then there is no window in which both were measured,
 * so their correlation is undefined rather than low and the null distribution comes out empty.
 * <p>
 * Isotope peaks answer it from the other side. A monoisotopic trace and its <sup>13</sup>C partner are one elution
 * measured twice, so their correlation samples the distribution of "correlation between two profiles that genuinely
 * are the same elution". A cutoff must never be stricter than that, or it splits elutions we know to be identical,
 * and anything above it is by definition indistinguishable from a real co-elution - exactly the property a grouping of
 * precursors needs. That an isotope trace is weaker than its monoisotope and therefore noisier does not need
 * correcting: it makes the distribution a lower bound on what a genuine co-elution reaches, and a lower bound is what
 * a cutoff wants.
 * <p>
 * Three things happen to the raw pairs, in this order.
 * <ol>
 *     <li><b>Binned</b> by how many points the correlation used, split at {@link #FEW_POINTS_THRESHOLD}, and by the
 *     intensity of the weaker partner in multiples of the sample's noise level. The intensity is not a refinement but
 *     the dominant effect: measured on a real sample the fifth percentile runs from below zero near the noise to
 *     0.998 a hundredfold above it.</li>
 *     <li><b>Merged</b> with the neighbouring intensity bin while a bin holds fewer than {@link #MIN_OBSERVATIONS}
 *     pairs. Adjacent bins, never a pool over everything: pairs near the noise outnumber the rest, so a global
 *     fallback is a fallback into noise, which is the opposite of what a fallback should be.</li>
 *     <li><b>Discarded</b> if the surviving bin still cannot produce a correlation worth having, that is if its
 *     {@link #REFERENCE_QUANTILE} sits below {@link #MIN_MEANINGFUL_CORRELATION}. Near the noise a known co-elution
 *     correlates either well or not at all - about a quarter of those pairs come out below 0.5 - so nothing read
 *     there means anything and the honest answer is that the regime cannot be judged. Where that stops being true is
 *     {@link #minimumUsableIntensity}, the intensity a peak needs before its co-elution can be assessed at all.</li>
 * </ol>
 * Note what is deliberately <em>not</em> done: individual pairs below the minimum are never dropped from a bin. A
 * quantile of a distribution truncated at 0.5 can never fall below 0.5 and lands just above it, so filtering pairs
 * would manufacture the answer it was meant to measure.
 */
public class TraceCorrelationStatistics {

    /** correlations from fewer points than this are a different condition, as in {@code DiaCorrelationPValues} */
    public static final int FEW_POINTS_THRESHOLD = 6;

    /**
     * Lower bounds of the intensity bins, in multiples of the sample's noise level; the last bin holds everything
     * above the last bound. Fine enough to locate where a co-elution becomes measurable rather than only bracketing
     * it within a decade, and thin bins are merged afterwards, so being generous here costs nothing.
     */
    public static final double[] INTENSITY_MULTIPLES = {1d, 2d, 5d, 10d, 20d, 50d, 100d};

    /** number of intensity bins, one more than there are bounds */
    public static final int INTENSITY_BINS = INTENSITY_MULTIPLES.length + 1;

    /**
     * How many pairs a bin needs before it may be read, and equally how far the thin bins above it accumulate before
     * they stop absorbing the bin below.
     * <p>
     * Both roles pull the same way and the second is the binding one. The pairs above ten times the noise number in
     * the tens on an ordinary sample while the bin just above the noise holds hundreds, so a floor set too high stops
     * the strong tail from ever standing on its own and it swallows a weak bin it disagrees with; the pooled quantile
     * is then the weak bin's, it falls under {@link #MIN_MEANINGFUL_CORRELATION}, and a regime that was measured
     * perfectly well reports that it cannot be judged. At two hundred that happened to every bin of a real sample.
     * <p>
     * A hundred keeps the quantile a statistic rather than an extremum - the fifth percentile of a hundred pairs is
     * the fifth smallest, not the smallest - which is the property that would be lost by going much lower.
     */
    public static final int MIN_OBSERVATIONS = 100;

    /** below this a correlation is not evidence of anything, so a bin that cannot beat it is discarded */
    public static final double MIN_MEANINGFUL_CORRELATION = 0.5;

    /**
     * The correlation two profiles have to reach to count as one elution where this sample cannot say, and the floor
     * no learned cutoff may fall below, per datapoint condition: {@code {few, many}}.
     * <p>
     * The isotope distribution is a lower bound on what a genuine co-elution reaches, so read literally it would
     * license grouping at whatever the few-point regime happens to manage - which on a real sample is below zero:
     * the fifth percentile of pairs that are the same elution by construction is -0.92 below one times the noise and
     * -0.53 in the many-point condition. Following it down is the wrong risk to take. Grouping two distinct compounds
     * costs both of them, since the fragments of one are then reported for the other and neither spectrum is right;
     * failing to group two indistinguishable ones only forgoes the pollution accounting, and each still gets its
     * fragments. So learning may raise the bar and never lower it, and where there is nothing to learn these stand in.
     * <p>
     * <b>The values are chosen on that asymmetry rather than on a measurement, because no measurement separates
     * them.</b> On the eighteen sample dilution series, unique library structures at cosine 0.7 came out at 70 for
     * {0.8, 0.9}, 71 for {0.9, 0.9}, 66 for {0.8, 0.95}, 69 for {0.9, 0.95} and 72 for {0.95, 0.98} - a spread of six
     * that is not monotone in the bar and is the same size as the difference between two configurations that agree on
     * only half of what they identify. The metric cannot rank them, so the conservative bar is preferred. Note that
     * where the distribution *can* be read it asks for 0.950 to 0.991, so these are not stricter than a measured
     * standard, only stricter than the absence of one. Measured 2026-08-21, after the determinism and isotope fixes;
     * figures taken before those describe a different pipeline.
     * <p>
     * Per condition because a correlation over four points and one over forty are not the same measurement, and the
     * few-point one is the more lenient of the two because it is the noisier measurement, not because less is asked
     * of it.
     * <p>
     * These are the floor of this statistic and nothing more. What a caller does with a co-elution standard is the
     * caller's policy and belongs where that policy lives - a grouping of precursors asserts that two traces cannot be
     * told apart and needs a far stricter bar than this, while another consumer of the same distribution may not - so
     * no such minimum is enforced here. Ask {@link #hasLearnedCutoffFor} to tell a measurement from a fallback and
     * apply your own.
     */
    public static final double[] MINIMUM_CUTOFF = {0.9, 0.95};

    /** the quantile a bin is judged by, so which bins are usable does not depend on what a caller asks for */
    public static final double REFERENCE_QUANTILE = 0.05;

    /**
     * Upper bound on what is kept per bin, thinned evenly so the quantiles are unchanged. These statistics live in
     * the sample's {@link SampleStats} and are written to its store, and a crowded sample yields hundreds of
     * thousands of pairs, of which a few thousand describe the distribution just as well.
     */
    public static final int MAX_OBSERVATIONS = 4000;

    /** the mass difference of a <sup>13</sup>C isotope peak */
    public static final double CARBON_ISOTOPE_DELTA = 1.00335484;

    /**
     * The numbers of points each pair is also measured at. A DIA channel sees a peak four to nine times where the MS1
     * scans see it tens of times, so every pair is correlated over subsamples of its points as well: without that the
     * few-points condition could never be filled from densely sampled data.
     */
    private static final int[] TARGET_POINTS = {4, 8};

    /** raw pairs per bin, sorted ascending, indexed by {@link #binOf}; this is what the store keeps */
    private final float[][] raw;

    /** which merged group each bin ended up in, the groups themselves, and whether a group can be read at all */
    private final int[] groupOfBin;
    private final float[][] groups;
    private final boolean[] usable;

    /**
     * @param raw the pairs of every bin, sorted ascending, as {@link #binOf} indexes them. Public because the store
     *            reconstructs an instance from exactly this and because a caller has to be able to state a
     *            distribution to test against; the merging and discarding above are applied here, not by the caller.
     */
    public TraceCorrelationStatistics(float[][] raw) {
        if (raw.length != binCount()) {
            throw new IllegalArgumentException("expected " + binCount() + " bins, got " + raw.length);
        }
        this.raw = raw;
        this.groupOfBin = new int[binCount()];
        final List<float[]> resolved = new ArrayList<>();
        final List<Boolean> resolvedUsable = new ArrayList<>();
        for (int points = 0; points < 2; ++points) {
            resolve(points, resolved, resolvedUsable);
        }
        this.groups = resolved.toArray(float[][]::new);
        this.usable = new boolean[resolvedUsable.size()];
        for (int k = 0; k < usable.length; ++k) usable[k] = resolvedUsable.get(k);
    }

    /**
     * Turns the intensity bins of one datapoint condition into the groups that are actually read, and records for
     * every bin which group answers for it.
     * <p>
     * Two rules, in order. A bin holding too few pairs is merged with the bin below it, walking from the strongest
     * down, because the thin bins are the strong ones and they have to keep their own kind of company. Then usability
     * is forced to be monotone in intensity: a stronger bin cannot be unusable while a weaker one is usable, since
     * more signal cannot make a co-elution harder to see - if it comes out that way it is noise in the estimate, and
     * the offending bin is merged downwards until it is not.
     */
    private void resolve(int points, List<float[]> groups, List<Boolean> usable) {
        // one segment per bin, in ascending intensity, each remembering which bins it covers
        final List<FloatArrayList> values = new ArrayList<>();
        final List<IntArrayList> covers = new ArrayList<>();
        for (int intensity = 0; intensity < INTENSITY_BINS; ++intensity) {
            final int bin = points * INTENSITY_BINS + intensity;
            values.add(new FloatArrayList(raw[bin]));
            covers.add(IntArrayList.of(bin));
        }

        // too few pairs to stand alone: merge into the segment below, from the strongest downwards
        for (int k = values.size() - 1; k > 0; --k) {
            if (values.get(k).size() < MIN_OBSERVATIONS) mergeDown(values, covers, k);
        }

        final int firstGroup = groups.size();
        for (int k = 0; k < values.size(); ++k) {
            final float[] sorted = values.get(k).toFloatArray();
            Arrays.sort(sorted);
            groups.add(sorted);
            usable.add(isUsable(values.get(k)));
            for (int bin : covers.get(k)) groupOfBin[bin] = firstGroup + k;
        }

        // A stronger segment may not be unusable while a weaker one is usable: more signal cannot make a co-elution
        // harder to see. Such a segment is pointed at the nearest usable segment below it rather than merged into it.
        // Merging looks more natural but can destroy what it was meant to rescue - a large contradicting segment
        // drags the quantile of the segment below it under the minimum and both end up unusable - and it is not
        // needed: the cutoff of a weaker regime is a valid, conservative bound for a stronger one, since that is the
        // direction correlation moves in. Everything here is a lower bound anyway.
        int lastUsable = -1;
        for (int k = 0; k < values.size(); ++k) {
            final int group = firstGroup + k;
            if (usable.get(group)) {
                lastUsable = group;
            } else if (lastUsable >= 0) {
                for (int bin : covers.get(k)) groupOfBin[bin] = lastUsable;
            }
        }
    }

    /** folds segment {@code k} into the one below it, which then covers both bin ranges */
    private static void mergeDown(List<FloatArrayList> values, List<IntArrayList> covers, int k) {
        values.get(k - 1).addAll(values.get(k));
        covers.get(k - 1).addAll(covers.get(k));
        values.remove(k);
        covers.remove(k);
    }

    private static boolean isUsable(FloatArrayList values) {
        if (values.size() < MIN_OBSERVATIONS) return false;
        final float[] sorted = values.toFloatArray();
        Arrays.sort(sorted);
        return quantileOf(sorted, REFERENCE_QUANTILE) >= MIN_MEANINGFUL_CORRELATION;
    }

    /** an instance that knows nothing, so every lookup reports that it cannot answer */
    public static TraceCorrelationStatistics empty() {
        final float[][] empty = new float[binCount()][];
        Arrays.fill(empty, new float[0]);
        return new TraceCorrelationStatistics(empty);
    }

    public static int binCount() {
        return 2 * INTENSITY_BINS;
    }

    /** @return the raw pairs of one bin, sorted; this is what is written to the store */
    public float[] observations(int bin) {
        return raw[bin];
    }

    public boolean isEmpty() {
        for (float[] bin : raw) if (bin.length > 0) return false;
        return true;
    }

    /** @return the bin a pair belongs to, an index into {@link #observations} */
    public static int binOf(int datapoints, double weakerIntensity, double noiseLevel) {
        int intensity = 0;
        if (noiseLevel > 0 && weakerIntensity > 0) {
            final double multiple = weakerIntensity / noiseLevel;
            while (intensity < INTENSITY_MULTIPLES.length && multiple >= INTENSITY_MULTIPLES[intensity]) ++intensity;
        }
        return (datapoints < FEW_POINTS_THRESHOLD ? 0 : 1) * INTENSITY_BINS + intensity;
    }

    /**
     * @param datapoints      how many points the correlation being judged was computed from
     * @param weakerIntensity apex intensity of the weaker of the two traces
     * @param noiseLevel      the sample's noise level, which the intensity bins are measured against
     * @param quantile        which quantile of the same-elution distribution to return, e.g. 0.05
     * @return the correlation that this share of genuine co-elutions falls below, or NaN if this regime cannot be
     * judged at all - a different statement from a low correlation, and one that has to stay distinguishable from it
     */
    public double quantileOfSameElution(int datapoints, double weakerIntensity, double noiseLevel, double quantile) {
        final int group = groupOfBin[binOf(datapoints, weakerIntensity, noiseLevel)];
        return usable[group] ? quantileOf(groups[group], quantile) : Double.NaN;
    }

    /**
     * One distribution from several, by pooling their pairs bin by bin.
     * <p>
     * Sound because the bins are relative to each sample's own noise level rather than absolute: a pair from a
     * sensitive run and one from a dull run land in the same bin when they stand equally far above what their own run
     * can see, which is the condition the cutoff is a function of. Thinned per bin like any other, so pooling a large
     * experiment costs no more memory than one sample.
     */
    public static TraceCorrelationStatistics pooled(Collection<TraceCorrelationStatistics> parts) {
        final float[][] all = new float[binCount()][];
        for (int bin = 0; bin < binCount(); ++bin) {
            final FloatArrayList merged = new FloatArrayList();
            for (TraceCorrelationStatistics part : parts) {
                for (float value : part.observations(bin)) merged.add(value);
            }
            all[bin] = thin(merged);
        }
        return new TraceCorrelationStatistics(all);
    }

    /**
     * This distribution, with the bins of every datapoint condition it cannot read taken from {@code other}.
     * <p>
     * Per condition and not wholesale, because a sample can perfectly well calibrate its many-point correlations and
     * have nothing to say about its few-point ones. And a fallback rather than a merge: where this sample can read a
     * condition it keeps what it measured, since pooling in the weak pairs of every other sample would lower the
     * cutoff of exactly the samples that did not need help.
     */
    public TraceCorrelationStatistics fallingBackTo(TraceCorrelationStatistics other) {
        final float[][] combined = new float[binCount()][];
        boolean replaced = false;
        for (int points = 0; points < 2; ++points) {
            // any datapoint count of the condition answers for it
            final boolean readable = !Double.isNaN(minimumUsableIntensity(
                    points == 0 ? FEW_POINTS_THRESHOLD - 1 : FEW_POINTS_THRESHOLD));
            replaced |= !readable;
            for (int intensity = 0; intensity < INTENSITY_BINS; ++intensity) {
                final int bin = points * INTENSITY_BINS + intensity;
                combined[bin] = readable ? raw[bin] : other.raw[bin];
            }
        }
        // this instance itself when it needed no help, so a caller can tell whether anything was borrowed
        return replaced ? new TraceCorrelationStatistics(combined) : this;
    }

    /** the floor for a datapoint condition, see {@link #MINIMUM_CUTOFF} */
    public static double minimumCutoff(int datapoints) {
        return MINIMUM_CUTOFF[datapoints < FEW_POINTS_THRESHOLD ? 0 : 1];
    }

    /**
     * The correlation two profiles of this regime have to reach to count as one elution, as far as this statistic can
     * say: what was measured where something was, held up by {@link #MINIMUM_CUTOFF}, and that floor where nothing
     * was. Always a number, so no caller needs a default of its own - but a caller with a policy of its own should ask
     * {@link #hasLearnedCutoffFor} as well, since this cannot distinguish the two cases.
     */
    public double cutoffFor(int datapoints, double weakerIntensity, double noiseLevel, double quantile) {
        final double learned = quantileOfSameElution(datapoints, weakerIntensity, noiseLevel, quantile);
        final double floor = minimumCutoff(datapoints);
        return Double.isNaN(learned) ? floor : Math.max(floor, learned);
    }

    /**
     * Whether {@link #cutoffFor} answered from this experiment's own pairs or fell back to {@link #MINIMUM_CUTOFF}.
     * <p>
     * The distinction a caller needs to apply a policy: "co-elutions here reach only 0.6" and "nothing here could be
     * measured" are different statements, and a consumer that must not act without evidence cannot tell them apart
     * from the cutoff alone.
     */
    public boolean hasLearnedCutoffFor(int datapoints, double weakerIntensity, double noiseLevel) {
        return !Double.isNaN(quantileOfSameElution(datapoints, weakerIntensity, noiseLevel, REFERENCE_QUANTILE));
    }

    /**
     * The intensity a trace needs before its co-elution can be judged at all, as a multiple of the noise level.
     * <p>
     * Below it a known co-elution correlates well or not at all, and a low correlation cannot be told from an
     * unmeasurable one, so a precursor that weak cannot be distinguished from anything. That is an argument for not
     * treating it as a precursor rather than for guessing about it.
     *
     * @return the multiple, or NaN if no bin of this datapoint condition is usable
     */
    public double minimumUsableIntensity(int datapoints) {
        final int offset = (datapoints < FEW_POINTS_THRESHOLD ? 0 : 1) * INTENSITY_BINS;
        for (int intensity = 0; intensity < INTENSITY_BINS; ++intensity) {
            if (usable[groupOfBin[offset + intensity]]) {
                return intensity == 0 ? 0d : INTENSITY_MULTIPLES[intensity - 1];
            }
        }
        return Double.NaN;
    }

    private static double quantileOf(float[] sorted, double quantile) {
        if (sorted.length == 0) return Double.NaN;
        return sorted[Math.min(sorted.length - 1, Math.max(0, (int) Math.floor(quantile * sorted.length)))];
    }

    /**
     * Learns the statistics from the isotope pairs of a sample, which needs nothing but the picked traces.
     * <p>
     * Isotope detection proper runs much later in the pipeline, so the pairs are found here directly: a trace one
     * <sup>13</sup>C heavier, overlapping in retention time, and weaker than its partner. A pair that is not really
     * an isotope pair is either a genuine co-elution anyway, in which case it belongs in the distribution, or an
     * accident, in which case it lowers the distribution and makes the bound more conservative.
     *
     * @param noiseLevel the scalar the intensity bins are measured against, see {@link #medianNoiseLevel}
     */
    public static TraceCorrelationStatistics learn(ProcessedSample sample, Deviation deviation, double noiseLevel) {
        final FloatArrayList[] observations = new FloatArrayList[binCount()];
        for (int k = 0; k < observations.length; ++k) observations[k] = new FloatArrayList();

        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
            final double mz = trace.averagedMz() + CARBON_ISOTOPE_DELTA;
            final double allowed = deviation.absoluteFor(mz);
            final List<ContiguousTrace> heavier = sample.getStorage().getTraceStorage()
                    .getContigousTraces(mz - allowed, mz + allowed, trace.startId(), trace.endId());
            for (ContiguousTrace isotope : heavier) {
                // the heavier one has to be the weaker one, or this is not a monoisotopic peak and its isotope
                if (isotope.apexIntensity() >= trace.apexIntensity()) continue;
                observe(observations, trace, isotope, noiseLevel);
            }
        }

        final float[][] raw = new float[observations.length][];
        for (int k = 0; k < observations.length; ++k) raw[k] = thin(observations[k]);
        return new TraceCorrelationStatistics(raw);
    }

    /** the sample's noise level as one number, which is what the intensity bins are relative to */
    public static double medianNoiseLevel(SampleStats stats) {
        final float[] perScan = stats.getNoiseLevelPerScan();
        if (perScan == null || perScan.length == 0) return stats.getMs2NoiseLevel();
        final float[] sorted = perScan.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    /** one pair, contributing an observation at every point count it can support */
    private static void observe(FloatArrayList[] observations, Trace mono, Trace isotope, double noiseLevel) {
        final int from = Math.max(mono.startId(), isotope.startId());
        final int to = Math.min(mono.endId(), isotope.endId());
        final int available = to - from + 1;
        if (available < 3) return;
        final double weaker = Math.min(apexWithin(mono, from, to), apexWithin(isotope, from, to));
        if (!(weaker > 0)) return;

        record(observations, profile(mono, from, to, 1), profile(isotope, from, to, 1), weaker, noiseLevel);
        for (int target : TARGET_POINTS) {
            final int stride = available / target;
            if (stride < 2) continue;
            record(observations, profile(mono, from, to, stride), profile(isotope, from, to, stride), weaker,
                    noiseLevel);
        }
    }

    private static void record(FloatArrayList[] observations, double[] left, double[] right, double weaker,
                               double noiseLevel) {
        if (left.length < 3) return;
        final double correlation = Statistics.pearson(left, right);
        if (!Double.isFinite(correlation)) return;
        observations[binOf(left.length, weaker, noiseLevel)].add((float) correlation);
    }

    private static double[] profile(Trace trace, int from, int to, int stride) {
        final int length = (to - from) / stride + 1;
        final double[] profile = new double[length];
        for (int k = 0; k < length; ++k) profile[k] = trace.intensity(from + k * stride);
        return profile;
    }

    private static double apexWithin(Trace trace, int from, int to) {
        double apex = 0d;
        for (int k = from; k <= to; ++k) apex = Math.max(apex, trace.intensity(k));
        return apex;
    }

    /** sorted, and thinned to {@link #MAX_OBSERVATIONS} evenly so that the quantiles survive */
    private static float[] thin(FloatArrayList values) {
        final float[] sorted = values.toFloatArray();
        Arrays.sort(sorted);
        if (sorted.length <= MAX_OBSERVATIONS) return sorted;
        final float[] thinned = new float[MAX_OBSERVATIONS];
        final double stride = sorted.length / (double) MAX_OBSERVATIONS;
        for (int k = 0; k < MAX_OBSERVATIONS; ++k) {
            thinned[k] = sorted[Math.min(sorted.length - 1, (int) (k * stride))];
        }
        return thinned;
    }

    /** what each bin holds, which group it ended up in, and whether that group can be read at all */
    public String describe() {
        final StringBuilder buf = new StringBuilder("correlation of known same elutions\n");
        for (int points = 0; points < 2; ++points) {
            final int datapoints = points == 0 ? 3 : 20;
            final double minimum = minimumUsableIntensity(datapoints);
            buf.append(String.format(Locale.US, "  %s points, usable from %s x noise upwards%n",
                    points == 0 ? "few " : "many",
                    Double.isNaN(minimum) ? "no" : String.format(Locale.US, "%.0f", minimum)));
            for (int intensity = 0; intensity < INTENSITY_BINS; ++intensity) {
                final int bin = points * INTENSITY_BINS + intensity;
                final int group = groupOfBin[bin];
                buf.append(String.format(Locale.US, "    %-11s %6d pairs, in a group of %6d, %s%n",
                        intensity < INTENSITY_MULTIPLES.length
                                ? String.format(Locale.US, "%.0f-%.0fx", intensity == 0 ? 0d
                                        : INTENSITY_MULTIPLES[intensity - 1], INTENSITY_MULTIPLES[intensity])
                                : String.format(Locale.US, "above %.0fx",
                                        INTENSITY_MULTIPLES[INTENSITY_MULTIPLES.length - 1]),
                        raw[bin].length, groups[group].length,
                        usable[group] ? String.format(Locale.US, "%.0f%% quantile %.3f",
                                100 * REFERENCE_QUANTILE, quantileOf(groups[group], REFERENCE_QUANTILE))
                                : "not usable"));
            }
        }
        return buf.toString();
    }
}
