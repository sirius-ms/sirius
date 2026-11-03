package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.Trace;

/**
 * Samples an MS1 trace at the times a DIA channel measured.
 * <p>
 * A precursor trace lives on the MS1 axis of its sample, a fragment trace on the axis of its channel, and the
 * two tick at completely different rates - 0.2 s against 2.24 s in the files we tested against. Their intensity
 * arrays therefore cannot be compared element by element; the precursor first has to be evaluated at the
 * channel's scan times.
 * <p>
 * The value between two MS1 scan points is interpolated linearly rather than averaged over the channel's
 * sampling interval. An MS2 scan looks at the eluting compound during its own short accumulation, not across
 * the whole cycle, so averaging would smooth the precursor more than the fragment is smoothed - and comparing
 * two differently smoothed versions of the same peak is exactly the kind of artefact that makes correlations
 * misleading.
 */
public final class Ms1TraceResampler {

    private Ms1TraceResampler() {
    }

    /**
     * @param trace      a trace on any retention time axis, usually an MS1 trace of the sample
     * @param targetAxis the axis to sample it on, usually the axis of a {@link DiaChannel}
     * @param from       first index of {@code targetAxis} to sample at, inclusive
     * @param to         last index of {@code targetAxis} to sample at, inclusive
     * @return intensities of the trace at the retention times of the target axis, zero wherever the target axis
     * lies outside the trace
     */
    public static double[] resample(Trace trace, ScanPointMapping targetAxis, int from, int to) {
        if (from < 0 || to >= targetAxis.length() || from > to) {
            throw new IllegalArgumentException("Invalid range " + from + " to " + to
                    + " for an axis of length " + targetAxis.length());
        }
        final double[] resampled = new double[to - from + 1];
        final int start = trace.startId(), end = trace.endId();
        final double traceStartRt = trace.retentionTime(start);
        final double traceEndRt = trace.retentionTime(end);

        // both sequences are sorted by retention time, so one pass with a moving cursor is enough
        int cursor = start;
        for (int k = from; k <= to; ++k) {
            final double rt = targetAxis.getRetentionTimeAt(k);
            if (rt < traceStartRt || rt > traceEndRt) continue; // outside the trace: stays zero
            while (cursor < end && trace.retentionTime(cursor + 1) < rt) ++cursor;
            if (cursor >= end) {
                // rt is at, or numerically indistinguishable from, the last scan point
                resampled[k - from] = trace.intensity(end);
                continue;
            }
            final double leftRt = trace.retentionTime(cursor);
            final double rightRt = trace.retentionTime(cursor + 1);
            final double span = rightRt - leftRt;
            if (span <= 0) {
                resampled[k - from] = trace.intensity(cursor);
            } else {
                final double weight = (rt - leftRt) / span;
                resampled[k - from] = trace.intensity(cursor) * (1d - weight) + trace.intensity(cursor + 1) * weight;
            }
        }
        return resampled;
    }
}
