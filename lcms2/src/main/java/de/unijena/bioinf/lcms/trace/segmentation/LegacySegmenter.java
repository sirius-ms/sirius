package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.Maxima;
import de.unijena.bioinf.lcms.trace.Trace;

import java.util.Arrays;

public class LegacySegmenter implements TraceSegmenter {
    @Override
    public Extrema detectExtrema(Trace trace) {
        double[] signal = getSignal(trace);
        de.unijena.bioinf.lcms.Extrema extrema = new Maxima(signal).toExtrema();
        final int[] ids = new int[extrema.numberOfExtrema()];
        final boolean[] maxima = new boolean[extrema.numberOfExtrema()];
        for (int k=0, n = extrema.numberOfExtrema(); k<n; ++k) {
            ids[k] = extrema.getIndexAt(k) + trace.startId();
            maxima[k] = !extrema.isMinimum(k);
        }
        return new Extrema(ids, maxima);
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
