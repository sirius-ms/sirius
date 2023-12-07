package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.Maxima;
import de.unijena.bioinf.lcms.trace.Trace;

import javax.swing.text.Segment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LegacySegmenter implements TraceSegmenter {

    public List<TraceSegment> detectSegments(Trace trace) {
        double[] signal = getSignal(trace);
        de.unijena.bioinf.lcms.Extrema extrema = new Maxima(signal).toExtrema();
        final List<TraceSegment> output = new ArrayList<>();
        for (int k=0, n = extrema.numberOfExtrema(); k<n; ++k) {
            int index = extrema.getIndexAt(k);
            if (!extrema.isMinimum(k)) {
                // the previous minimum is the left edge
                int prev=Math.max(0, index-1);
                int next = Math.min(index+1, extrema.numberOfExtrema()-1);
                output.add(new TraceSegment(index+ trace.startId(), prev+trace.startId(), next+trace.startId()));
            };
        }
        return output;
    }

    @Override
    public int[] detectMaxima(Trace trace) {
        return Arrays.stream(new Maxima(getSignal(trace)).getMaximaLocations()).map(x->x+trace.startId()).toArray();
    }

    private double[] getSignal(Trace trace) {
        final double[] signal = new double[trace.length()];
        for (int j=0, k=trace.startId(), n = trace.endId(); k <= n; ++k) {
            signal[j++] = trace.intensity(k);
        }
        return signal;
    }
}
