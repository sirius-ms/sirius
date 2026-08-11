package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One preliminary feature of an MS1 trace, seen from one DIA channel: the thing fragments get assigned to.
 * <p>
 * A precursor is a feature rather than a whole trace, because a trace can carry several features and fragments
 * belong to one of them, not to the trace. The segmentation used here is the preliminary one from trace picking;
 * the final feature boundaries only exist after alignment, which is why stage one of the assignment is a
 * candidate generator and not the final word.
 * <p>
 * A precursor m/z covered by more than one channel - overlapping isolation windows, or several collision
 * energies - yields one instance per channel.
 */
public class PrecursorFeature {

    @Getter
    private final ContiguousTrace trace;

    @Getter
    private final TraceSegment segment;

    /** the scan points of the channel covering this feature */
    @Getter
    private final ChannelWindow window;

    @Getter
    private final int channelId;

    /** index into the channel's axis closest to the apex of this feature */
    @Getter
    private final int apexChannelIndex;

    private PrecursorFeature(ContiguousTrace trace, TraceSegment segment, ChannelWindow window, int channelId,
                             int apexChannelIndex) {
        this.trace = trace;
        this.segment = segment;
        this.window = window;
        this.channelId = channelId;
        this.apexChannelIndex = apexChannelIndex;
    }

    /**
     * @return the feature as seen from this channel, or empty if the channel does not cover its m/z or does not
     * sample it often enough to describe a shape
     */
    public static Optional<PrecursorFeature> of(DiaChannel channel, ContiguousTrace ms1Trace, TraceSegment segment) {
        if (!channel.covers(ms1Trace.averagedMz())) return Optional.empty();
        return onAxisOf(channel, ms1Trace, segment);
    }

    /**
     * The same, but without requiring the channel to cover the m/z. Used to build decoys: a precursor outside a
     * channel's isolation windows cannot have contributed any of that channel's fragments, so pairing the two
     * gives an edge that is null by construction while keeping everything else - the retention times, the window,
     * the structural criteria - exactly as it is for a real edge.
     */
    static Optional<PrecursorFeature> decoy(DiaChannel channel, ContiguousTrace ms1Trace, TraceSegment segment) {
        return onAxisOf(channel, ms1Trace, segment);
    }

    private static Optional<PrecursorFeature> onAxisOf(DiaChannel channel, ContiguousTrace ms1Trace, TraceSegment segment) {
        return ChannelWindow.of(channel, ms1Trace, segment).map(window -> new PrecursorFeature(
                ms1Trace, segment, window, channel.getChannelId(),
                channel.channelIndexForRetentionTime(ms1Trace.retentionTime(segment.apex))));
    }

    /**
     * All features of all given traces that this channel could have fragmented. Traces without a segmentation
     * are skipped: without features there is nothing to assign fragments to.
     */
    public static List<PrecursorFeature> of(DiaChannel channel, Iterable<ContiguousTrace> ms1Traces) {
        final List<PrecursorFeature> features = new ArrayList<>();
        for (ContiguousTrace trace : ms1Traces) {
            if (trace.getSegments() == null) continue;
            for (TraceSegment segment : trace.getSegments()) {
                of(channel, trace, segment).ifPresent(features::add);
            }
        }
        return features;
    }

    public double getMz() {
        return trace.averagedMz();
    }

    public double getRetentionTime() {
        return trace.retentionTime(segment.apex);
    }

    public float getApexIntensity() {
        return trace.intensity(segment.apex);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US, "PrecursorFeature(m/z %.4f, rt %.1f s, channel %d, window %d-%d)",
                getMz(), getRetentionTime(), channelId, window.from(), window.to());
    }
}
