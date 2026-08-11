package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One feature of an MS1 trace, seen from one DIA channel: the thing fragments get assigned to.
 * <p>
 * A precursor is a feature rather than a whole trace, because a trace can carry several features and fragments
 * belong to one of them, not to the trace.
 * <p>
 * Which segmentation the feature comes from is deliberately not fixed here. Stage one builds these from the
 * preliminary segments of trace picking, which is why it is a candidate generator and not the final word; stage two
 * builds them from the final feature boundaries that only exist after alignment, over the raw trace of one sample.
 * Both need the same thing of a trace - retention times, intensities and an averaged mass - so this works on
 * {@link Trace} rather than on the picked {@link ContiguousTrace}, and keeps the trace's identifier separately for
 * whoever has to point back at it.
 * <p>
 * A precursor m/z covered by more than one channel - overlapping isolation windows, or several collision
 * energies - yields one instance per channel.
 */
public class PrecursorFeature {

    @Getter
    private final Trace trace;

    /** identifier of the trace within its sample, or -1 for a trace that is not stored under one */
    @Getter
    private final int traceUid;

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

    private PrecursorFeature(Trace trace, int traceUid, TraceSegment segment, ChannelWindow window, int channelId,
                             int apexChannelIndex) {
        this.trace = trace;
        this.traceUid = traceUid;
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
        return of(channel, ms1Trace, ms1Trace.getUid(), segment);
    }

    /**
     * @param traceUid identifier of the trace within its sample, or -1 if it is not stored under one
     * @return the feature as seen from this channel, or empty if the channel does not cover its m/z or does not
     * sample it often enough to describe a shape
     */
    public static Optional<PrecursorFeature> of(DiaChannel channel, Trace ms1Trace, int traceUid, TraceSegment segment) {
        if (!channel.covers(ms1Trace.averagedMz())) return Optional.empty();
        return onAxisOf(channel, ms1Trace, traceUid, segment);
    }

    /**
     * The same, but without requiring the channel to cover the m/z. Used to build decoys: a precursor outside a
     * channel's isolation windows cannot have contributed any of that channel's fragments, so pairing the two
     * gives an edge that is null by construction while keeping everything else - the retention times, the window,
     * the structural criteria - exactly as it is for a real edge.
     */
    static Optional<PrecursorFeature> decoy(DiaChannel channel, ContiguousTrace ms1Trace, TraceSegment segment) {
        return onAxisOf(channel, ms1Trace, ms1Trace.getUid(), segment);
    }

    private static Optional<PrecursorFeature> onAxisOf(DiaChannel channel, Trace ms1Trace, int traceUid,
                                                      TraceSegment segment) {
        return ChannelWindow.of(channel, ms1Trace, segment).map(window -> new PrecursorFeature(
                ms1Trace, traceUid, segment, window, channel.getChannelId(),
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
