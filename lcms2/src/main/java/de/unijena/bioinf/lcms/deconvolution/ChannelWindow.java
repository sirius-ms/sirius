package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;

import java.util.Optional;

/**
 * A range of scan points of one {@link DiaChannel}, used as the range over which elution profiles are compared.
 * <p>
 * Comparisons are anchored on a feature of a precursor rather than on a whole trace: a trace can carry several
 * features, and a background mass carries none of its own, so correlating traces as a whole conflates things
 * that have nothing to do with each other. The window is therefore derived from a preliminary
 * {@link TraceSegment} of the precursor, translated from the MS1 axis onto the much coarser axis of the channel.
 */
public record ChannelWindow(int from, int to) {

    public ChannelWindow {
        if (from > to) throw new IllegalArgumentException("empty window: " + from + " to " + to);
    }

    /**
     * Translates a segment of an MS1 trace onto the scan points of a channel.
     *
     * @return the window, or empty if the segment does not reach the channel or is too narrow to be sampled by
     * at least {@link DiaTraceCorrelation#MIN_POINTS} of its scan points
     */
    public static Optional<ChannelWindow> of(DiaChannel channel, Trace ms1Trace, TraceSegment segment) {
        final double startRt = ms1Trace.retentionTime(segment.leftEdge);
        final double endRt = ms1Trace.retentionTime(segment.rightEdge);
        // the channel index closest in retention time; may be clamped to either end of the axis, so we have to
        // check afterwards that the window really lies within the segment
        final int from = channel.channelIndexForRetentionTime(startRt);
        final int to = channel.channelIndexForRetentionTime(endRt);
        if (to < from) return Optional.empty();
        final double windowStartRt = channel.getAxis().getRetentionTimeAt(from);
        final double windowEndRt = channel.getAxis().getRetentionTimeAt(to);
        // no overlap at all, i.e. the segment lies entirely before or after everything this channel measured
        if (windowEndRt < startRt || windowStartRt > endRt) return Optional.empty();
        final ChannelWindow window = new ChannelWindow(from, to);
        return window.length() >= DiaTraceCorrelation.MIN_POINTS ? Optional.of(window) : Optional.empty();
    }

    public int length() {
        return to - from + 1;
    }

    /**
     * @return the scan point at which the trace is highest inside this window, or -1 if it was not measured here at
     * all. Note that this is the trace's apex <em>within the window</em>, which for a fragment of a channel that
     * samples once per cycle is often not the point where the compound really peaked.
     */
    public int apexIndexIn(Trace trace) {
        int apex = -1;
        double best = 0d;
        for (int k = Math.max(from, trace.startId()), end = Math.min(to, trace.endId()); k <= end; ++k) {
            final double intensity = trace.intensity(k);
            if (intensity > best) {
                best = intensity;
                apex = k;
            }
        }
        return apex;
    }

    /** @return the highest intensity the trace reaches inside this window, or zero */
    public double apexIntensityIn(Trace trace) {
        final int apex = apexIndexIn(trace);
        return apex < 0 ? 0d : trace.intensity(apex);
    }

    public boolean contains(int channelIndex) {
        return channelIndex >= from && channelIndex <= to;
    }

    public boolean overlaps(ChannelWindow other) {
        return from <= other.to && other.from <= to;
    }

    /**
     * @return the smallest window containing both. Used to give a cluster of precursor features that overlap in
     * retention time one common comparison range.
     */
    public ChannelWindow union(ChannelWindow other) {
        return new ChannelWindow(Math.min(from, other.from), Math.max(to, other.to));
    }
}
