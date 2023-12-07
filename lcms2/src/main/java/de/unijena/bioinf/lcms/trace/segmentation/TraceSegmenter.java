package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.trace.Trace;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;

public interface TraceSegmenter extends ApexDetection {

    public List<TraceSegment> detectSegments(Trace trace);

    default int[] detectMaxima(Trace trace) {
        return detectSegments(trace).stream().mapToInt(x->x.apex).sorted().toArray();
    }
}
