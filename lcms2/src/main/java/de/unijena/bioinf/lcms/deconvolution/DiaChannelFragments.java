package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.NoSegmentation;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.utils.Tracker;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Picks the fragment chromatograms of one DIA channel.
 * <p>
 * Picking happens once for the whole channel rather than once per precursor feature. Seeding from the scans of
 * every feature separately would repeat most of the work: a channel's scan holds on the order of a hundred peaks
 * and a feature covers a handful of scans, so with thousands of features per channel that is a million attempts,
 * against roughly fifty thousand when the channel's own scans are walked once. The features then simply select
 * from the result.
 * <p>
 * Only peaks above the channel's noise level are used as seeds. The picker already discards anything it cannot
 * follow across three scan points, but seeding from noise would still cost most of the runtime.
 */
public final class DiaChannelFragments {


    /**
     * @param traces        the fragment chromatograms of the channel
     * @param seedsTried    peaks that were used as a seed
     * @param seedsBelowNoise peaks that were skipped because they were at or below the noise level
     */
    public record Result(List<ContiguousTrace> traces, int seedsTried, int seedsBelowNoise) {
        public int numberOfTraces() {
            return traces.size();
        }
    }

    private DiaChannelFragments() {
    }

    /**
     * Picks with {@link NoSegmentation}: the range a fragment is compared over comes from the precursor feature it
     * is tested against, so segmenting the fragment itself would be work nobody reads.
     */
    public static Result pickAll(ProcessedSample sample, DiaChannel channel, float noiseLevel) {
        return pickAll(sample, channel, noiseLevel, new NoSegmentation());
    }

    /**
     * @param minScanPoints how many of the channel's scan points a fragment has to be followed across, see
     *                      {@link de.unijena.bioinf.lcms.traceextractor.AbstractTracePicker#getMinScanPoints}
     */
    public static Result pickAll(ProcessedSample sample, DiaChannel channel, float noiseLevel, int minScanPoints) {
        return pickAll(sample, channel, noiseLevel, new NoSegmentation(), minScanPoints);
    }

    /**
     * @param noiseLevel noise level of this channel, see {@link DiaChannelNoise}
     * @param segmenter  used by the picker while following a trace
     */
    public static Result pickAll(ProcessedSample sample, DiaChannel channel, float noiseLevel,
                                 TraceSegmentationStrategy segmenter) {
        return pickAll(sample, channel, noiseLevel, segmenter, DiaDeconvolution.DEFAULT_MIN_FRAGMENT_SCAN_POINTS);
    }

    public static Result pickAll(ProcessedSample sample, DiaChannel channel, float noiseLevel,
                                 TraceSegmentationStrategy segmenter, int minScanPoints) {
        return pickAll(sample, channel, noiseLevel, segmenter, minScanPoints, new Tracker.NOOP());
    }

    public static Result pickAll(ProcessedSample sample, DiaChannel channel, float noiseLevel,
                                 TraceSegmentationStrategy segmenter, int minScanPoints, Tracker tracker) {
        final boolean tracked = tracker.tracksDia();
        final Ms2ChannelTraceCache cache = new Ms2ChannelTraceCache(sample, channel);
        final Ms2ChannelTracePicker picker = new Ms2ChannelTracePicker(sample, channel, noiseLevel, cache, segmenter);
        picker.setMinScanPoints(minScanPoints);
        int seedsTried = 0, seedsBelowNoise = 0;


        for (int scan = 0; scan < channel.numberOfScans(); ++scan) {
            final SimpleSpectrum spectrum = picker.getSpectrum(scan);
            if (spectrum == null) continue;
            for (int peak = 0; peak < spectrum.size(); ++peak) {
                final int index = scan;
                if (tracked) {
                    tracker.ms2ScanPeak(sample, channel.describe(), channel.getAxis().getRetentionTimeAt(index),
                            spectrum.getMzAt(peak), (float) spectrum.getIntensityAt(peak), noiseLevel);
                }
                if (spectrum.getIntensityAt(peak) <= noiseLevel) {
                    ++seedsBelowNoise;
                    continue;
                }
                ++seedsTried;
                final Optional<ContiguousTrace> picked = picker.detectTrace(index, spectrum.getMzAt(peak));
                if (tracked) {
                    tracker.ms2TracePicked(sample, channel.describe(), spectrum.getMzAt(peak),
                            channel.getAxis().getRetentionTimeAt(index), picked.orElse(null));
                }
            }
        }
        // The traces come from the storage rather than from what the lookups above handed back. A peak already
        // covered by a trace makes the picker return that trace instead of picking a new one, and where two traces
        // cover the same peak - about one lookup in a hundred - which one comes back depends on the order the
        // rectangle index returns them in. Collecting the answers therefore both omitted traces that were picked but
        // never handed back, and did so differently between two runs on the same data.
        return new Result(cache.all(), seedsTried, seedsBelowNoise);
    }
}
