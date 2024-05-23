package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.Maxima;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LegacySegmenter implements TraceSegmentationStrategy {

    public List<TraceSegment> detectSegments(Trace trace, double noiseLevel) {
        double[] signal = getSignal(trace);
        Maxima maxima = new Maxima(signal);
        float N = (float)noiseLevel;
        maxima.split(N, 0.05);
        de.unijena.bioinf.lcms.Extrema extrema = maxima.toExtrema();
        if (!extrema.isMinimum(extrema.numberOfExtrema()-1)) {
            // find minimum
            int k=extrema.lastExtremum();
            double min = trace.intensity(k+ trace.startId());
            int mindex = k;
            for (k=extrema.lastExtremum()+1; k < trace.length(); ++k) {
                float i = trace.intensity(k + trace.startId());
                if (i < min) {
                    mindex = k;
                    min = trace.intensity(k+ trace.startId());
                }
                if (i < N) break;
            }
            if (mindex > extrema.lastExtremum()) extrema.addExtremum(mindex, min);
        }
        final List<TraceSegment> output = new ArrayList<>();
        for (int k=0, n = extrema.numberOfExtrema(); k<n; ++k) {
            int index = extrema.getIndexAt(k);
            if (!extrema.isMinimum(k)) {
                // the previous minimum is the left edge
                int prev=extrema.getIndexAt(Math.max(0, k-1));
                int next = extrema.getIndexAt(Math.min(k+1, extrema.numberOfExtrema()-1));
                output.add(new TraceSegment(index+ trace.startId(), prev+trace.startId(), next+trace.startId()));
            };
        }
        return output;
    }

    @Override
    public int[] detectMaxima(SampleStats stats, Trace trace) {
        Maxima maxima = new Maxima(getSignal(trace));
        float N = stats.noiseLevel(trace.apex());
        maxima.split(N, 0.05);
        return Arrays.stream(maxima.getMaximaLocations()).map(x->x+trace.startId()).toArray();
    }

    private double[] getSignal(Trace trace) {
        final double[] signal = new double[trace.length()];
        for (int j=0, k=trace.startId(), n = trace.endId(); k <= n; ++k) {
            signal[j++] = trace.intensity(k);
        }
        return signal;
    }
}
