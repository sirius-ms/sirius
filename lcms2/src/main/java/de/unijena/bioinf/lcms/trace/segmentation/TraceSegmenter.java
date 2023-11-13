package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.trace.Trace;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public interface TraceSegmenter extends ApexDetection {

    public Extrema detectExtrema(Trace trace);


    default int[] detectMaxima(Trace trace) {
        Extrema extrema = detectExtrema(trace);
        IntArrayList xs = new IntArrayList(extrema.size()/2);
        for (int i=0; i < extrema.size(); ++i) {
            if (extrema.isMaximum(i)) {
                xs.add(extrema.getExtremumAt(i));
            }
        }
        return xs.toIntArray();
    }
}
