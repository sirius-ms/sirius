package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.TraceStats;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Recovers the DIA acquisition scheme of a sample from its MS2 spectrum headers.
 * <p>
 * Everything vendor specific ends up here, so the builder is deliberately paranoid about what it trusts:
 * <ul>
 *     <li>It does not rely on the order in which spectra appear in the file. Scans are grouped by their
 *     isolation windows and then sorted by retention time.</li>
 *     <li>It does not rely on the order in which co-isolated precursors of a multiplexed scan are listed.</li>
 *     <li>It does not blindly trust the reference to the precursor scan. A reference is only used when its
 *     retention time is plausible, otherwise the closest MS1 scan in retention time is used instead.</li>
 *     <li>It does not compare window bounds for exact floating point equality.</li>
 * </ul>
 */
public class DiaScanIndexBuilder {

    private static final Logger log = LoggerFactory.getLogger(DiaScanIndexBuilder.class);

    /**
     * A channel that fires less often than this cannot carry a chromatographic peak shape, so there is nothing
     * we could correlate against an MS1 trace. Such scans are counted and reported, but not used.
     */
    public static final int MIN_SCANS_PER_CHANNEL = 5;

    /**
     * How often a window has to be revisited within one chromatographic peak before its scans can be deconvoluted.
     * <p>
     * The question a DIA acquisition has to answer is not how wide its isolation windows are but whether its scans
     * can be correlated, and that is decided per window: consecutive visits must fall inside the same elution.
     * A data dependent run re-selecting the same precursor also produces a recurring window, so width cannot tell
     * the two apart - measured on real files, two genuine SWATH acquisitions reach about eleven and four points per
     * peak while two data dependent ones reach about one.
     * <p>
     * Three is what the correlation needs at all and is also as high as this can go: the four of the SWATH run our
     * DIA validation rests on leaves no room above it.
     */
    public static final int MIN_POINTS_PER_PEAK = 3;

    /**
     * What a chromatographic peak spans, in units of the peak width the samples measure.
     * <p>
     * {@code TraceStats} measures a peak as the intensity weighted standard deviation of its retention time, a
     * gaussian sigma rather than a width at half maximum, and a peak is usable across roughly plus or minus two of
     * those. The factor is written down here rather than folded into {@link #MIN_POINTS_PER_PEAK} because the two
     * are different claims: one is about what the instrument must deliver, the other about what a peak is.
     */
    public static final double PEAK_SPAN_IN_SIGMA = 4d;

    /**
     * The share of a sample's MS2 scans that has to end up in deconvolutable channels before the sample is treated
     * as a DIA measurement at all.
     * <p>
     * A per window rule alone would deconvolute the few dense windows of an ordinary data dependent run, and that is
     * worse than leaving them alone: the log p-values every assignment is judged by come from decoy edges collected
     * over the channels being processed, so a handful of channels means a null distribution built from almost
     * nothing, and a significance floor that cannot be cleared honestly rather than one that is merely hard to clear.
     * Below this share the sample is not DIA and all of its scans belong to the data dependent path.
     */
    public static final double MIN_SCAN_COVERAGE = 0.05;

    /**
     * How far the referenced precursor scan may be away from its MS2 scan, in multiples of the median MS1 scan
     * distance, before we distrust the reference and fall back to retention time.
     */
    private static final double MAX_PARENT_DISTANCE_IN_MS1_STEPS = 4d;

    /**
     * @param sample a sample whose spectra have already been parsed
     * @return the acquisition scheme, or empty if the sample does not look like DIA at all
     */
    public static Optional<DiaScanIndex> build(ProcessedSample sample) {
        final List<Ms2SpectrumHeader> headers = new ArrayList<>();
        for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) headers.add(header);
        Int2ObjectMap<float[]> multiplexed = sample.getMultiplexedIsolationWindows();
        // the peak width has to be measured here rather than read from the sample statistics: the merged
        // expectedPeakWidth is only set after merging, which is long after this decision is made
        return build(headers, multiplexed == null ? Int2ObjectMaps.emptyMap() : multiplexed, sample.getMapping(),
                TraceStats.averagePeakWidthOf(sample));
    }

    /**
     * @param headers            all MS2 spectrum headers of the sample, in any order
     * @param multiplexedWindows for every MS2 scan that co-isolates more than one window, the absolute m/z bounds
     *                           of all of its windows, interleaved as [lo,hi,lo,hi,...] and in any order. Scans
     *                           that are missing here are assumed to isolate the single window of their header.
     * @param ms1Mapping         the retention time axis of the sample's MS1 scans
     * @return the acquisition scheme, or empty if no usable channel was found
     */
    public static Optional<DiaScanIndex> build(Iterable<Ms2SpectrumHeader> headers,
                                               Int2ObjectMap<float[]> multiplexedWindows,
                                               ScanPointMapping ms1Mapping, double peakWidth) {
        final Map<DiaChannelKey, List<Ms2SpectrumHeader>> groups = new HashMap<>();
        int droppedScans = 0, deconvolutedScans = 0;
        for (Ms2SpectrumHeader header : headers) {
            if (DiaPseudoSpectra.isDeconvoluted(header)) {
                // computed, not acquired: it carries an isolation window of its own and would otherwise be
                // counted as a measurement, inflating its channel or inventing new ones
                ++deconvolutedScans;
                continue;
            }
            final float[] bounds = absoluteBoundsOf(header, multiplexedWindows);
            if (bounds == null) {
                // no isolation window at all: this is not something we can deconvolute
                ++droppedScans;
                continue;
            }
            groups.computeIfAbsent(DiaChannelKey.of(bounds, header.getEnergy().orElse(null)), k -> new ArrayList<>()).add(header);
        }
        if (groups.isEmpty()) return Optional.empty();

        final double parentTolerance = MAX_PARENT_DISTANCE_IN_MS1_STEPS * medianScanInterval(ms1Mapping);
        // how long a window may go unvisited and still sample a peak MIN_POINTS_PER_PEAK times; zero disables the
        // rule, which is what an unmeasurable peak width has to mean - it is not evidence that nothing co-elutes
        final double maximumRevisitGap = peakWidth > 0
                ? PEAK_SPAN_IN_SIGMA * peakWidth / MIN_POINTS_PER_PEAK : 0d;
        int ms2WithWindow = 0;
        for (List<Ms2SpectrumHeader> group : groups.values()) ms2WithWindow += group.size();

        // canonical channel order, so that the result never depends on the order the scans were read in
        final List<DiaChannelKey> keys = new ArrayList<>(groups.keySet());
        Collections.sort(keys);

        final List<DiaChannel> channels = new ArrayList<>(keys.size());
        int repairedParents = 0;
        for (DiaChannelKey key : keys) {
            final List<Ms2SpectrumHeader> scans = groups.get(key);
            if (scans.size() < MIN_SCANS_PER_CHANNEL) {
                droppedScans += scans.size();
                log.debug("Ignoring DIA channel {} because it only contains {} scans.", key, scans.size());
                continue;
            }
            // and it has to be revisited inside a peak, or its scans are of unrelated elutions
            if (maximumRevisitGap > 0 && medianRevisitGap(scans) > maximumRevisitGap) {
                droppedScans += scans.size();
                log.debug("Ignoring DIA channel {}: revisited every {} s, which cannot sample a peak.",
                        key, medianRevisitGap(scans));
                continue;
            }
            // retention time is the one thing we can rely on
            scans.sort(Comparator.comparingDouble(Ms2SpectrumHeader::getRetentionTime)
                    .thenComparingInt(Ms2SpectrumHeader::getScanId)
                    .thenComparingInt(Ms2SpectrumHeader::getUid));

            final double[] retentionTimes = new double[scans.size()];
            final int[] ms2Uids = new int[scans.size()];
            final int[] toMs1Index = new int[scans.size()];
            for (int k = 0; k < scans.size(); ++k) {
                final Ms2SpectrumHeader scan = scans.get(k);
                retentionTimes[k] = scan.getRetentionTime();
                ms2Uids[k] = scan.getUid();
                final int parentId = scan.getParentId();
                if (parentId >= 0 && parentId < ms1Mapping.length()
                        && Math.abs(ms1Mapping.getRetentionTimeAt(parentId) - scan.getRetentionTime()) <= parentTolerance) {
                    toMs1Index[k] = parentId;
                } else {
                    toMs1Index[k] = ms1Mapping.idForRetentionTime(scan.getRetentionTime());
                    ++repairedParents;
                }
            }
            final float[] bounds = key.canonicalBounds();
            final float[] windowLo = new float[bounds.length / 2];
            final float[] windowHi = new float[bounds.length / 2];
            for (int k = 0; k < windowLo.length; ++k) {
                windowLo[k] = bounds[2 * k];
                windowHi[k] = bounds[2 * k + 1];
            }
            channels.add(new DiaChannel(channels.size(), windowLo, windowHi, scans.getFirst().getEnergy().orElse(null),
                    new ScanPointMapping(retentionTimes, ms2Uids, null), toMs1Index));
        }
        if (channels.isEmpty()) {
            log.debug("No DIA channel of this sample fires at least {} times; not treating it as DIA.", MIN_SCANS_PER_CHANNEL);
            return Optional.empty();
        }
        int claimedScans = 0;
        for (DiaChannel channel : channels) claimedScans += channel.numberOfScans();
        if (claimedScans < MIN_SCAN_COVERAGE * ms2WithWindow) {
            log.debug("Only {} of {} MS2 scans fall into deconvolutable windows, below {}; not treating it as DIA.",
                    claimedScans, ms2WithWindow, MIN_SCAN_COVERAGE);
            return Optional.empty();
        }
        return Optional.of(new DiaScanIndex(channels, droppedScans, repairedParents, deconvolutedScans));
    }

    /**
     * @return absolute m/z bounds of all windows co-isolated by this scan, interleaved as [lo,hi,lo,hi,...], or
     * null if the scan does not define an isolation window
     */
    private static float[] absoluteBoundsOf(Ms2SpectrumHeader header, Int2ObjectMap<float[]> multiplexedWindows) {
        final float[] multiplexed = multiplexedWindows.get(header.getUid());
        if (multiplexed != null && multiplexed.length >= 2) return multiplexed;

        final IsolationWindow window = header.getIsolationWindow().orElse(null);
        if (window == null || window.isUndefined()) return null;
        // the isolation window is stored relative to the precursor, so we need the m/z the instrument targeted
        double target = header.getTargetedMz();
        if (!Double.isFinite(target) || target <= 0) target = header.getPrecursorMz();
        if (!Double.isFinite(target) || target <= 0) return null;
        return new float[]{(float) (target + window.getLeftOffset()), (float) (target + window.getRightOffset())};
    }

    /** the median gap between consecutive visits of one window, in seconds */
    private static double medianRevisitGap(List<Ms2SpectrumHeader> scans) {
        final double[] retentionTimes = new double[scans.size()];
        for (int k = 0; k < retentionTimes.length; ++k) retentionTimes[k] = scans.get(k).getRetentionTime();
        Arrays.sort(retentionTimes);
        final double[] gaps = new double[retentionTimes.length - 1];
        for (int k = 1; k < retentionTimes.length; ++k) gaps[k - 1] = retentionTimes[k] - retentionTimes[k - 1];
        Arrays.sort(gaps);
        return gaps[gaps.length / 2];
    }

    private static double medianScanInterval(ScanPointMapping mapping) {
        if (mapping.length() < 2) return 0d;
        final double[] deltas = new double[mapping.length() - 1];
        for (int k = 1; k < mapping.length(); ++k) {
            deltas[k - 1] = mapping.getRetentionTimeAt(k) - mapping.getRetentionTimeAt(k - 1);
        }
        Arrays.sort(deltas);
        return deltas[deltas.length / 2];
    }
}
