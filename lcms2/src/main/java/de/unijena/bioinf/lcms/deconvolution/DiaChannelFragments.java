package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.NoSegmentation;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

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
     * @param noiseLevel noise level of this channel, see {@link DiaChannelNoise}
     * @param segmenter  used by the picker while following a trace
     */
    public static Result pickAll(ProcessedSample sample, DiaChannel channel, float noiseLevel,
                                 TraceSegmentationStrategy segmenter) {
        final Ms2ChannelTraceCache cache = new Ms2ChannelTraceCache(sample, channel);
        final Ms2ChannelTracePicker picker = new Ms2ChannelTracePicker(sample, channel, noiseLevel, cache, segmenter);
        final IntOpenHashSet seen = new IntOpenHashSet();
        final List<ContiguousTrace> traces = new ArrayList<>();
        int seedsTried = 0, seedsBelowNoise = 0;

        for (int scan = 0; scan < channel.numberOfScans(); ++scan) {
            final SimpleSpectrum spectrum = picker.getSpectrum(scan);
            if (spectrum == null) continue;
            for (int peak = 0; peak < spectrum.size(); ++peak) {
                if (spectrum.getIntensityAt(peak) <= noiseLevel) {
                    ++seedsBelowNoise;
                    continue;
                }
                ++seedsTried;
                final int index = scan;
                picker.detectTrace(index, spectrum.getMzAt(peak)).ifPresent(trace -> {
                    if (seen.add(trace.getUid())) traces.add(trace);
                });
            }
        }
        return new Result(traces, seedsTried, seedsBelowNoise);
    }
}
