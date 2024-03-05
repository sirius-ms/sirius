package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.ms.persistence.model.core.run.SampleStats;
import de.unijena.bioinf.lcms.trace.Trace;

import java.util.List;

public interface TraceSegmentationStrategy extends ApexDetection {

    public List<TraceSegment> detectSegments(SampleStats stats, Trace trace);

    default int[] detectMaxima(SampleStats stats, Trace trace) {
        return detectSegments(stats, trace).stream().mapToInt(x->x.apex).sorted().toArray();
    }
}
