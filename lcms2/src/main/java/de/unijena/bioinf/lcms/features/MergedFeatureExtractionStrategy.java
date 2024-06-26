package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;

public interface MergedFeatureExtractionStrategy {

    public TraceSegment[] extractMergedSegments(TraceSegmentationStrategy traceSegmenter, ProcessedSample mergedSample, MergedTrace mergedTrace);

    public TraceSegment[][] extractProjectedSegments(ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] mergedTraceSegments);

}
