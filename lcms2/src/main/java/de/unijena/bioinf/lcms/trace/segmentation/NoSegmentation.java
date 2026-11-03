package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.trace.Trace;

import java.util.List;

/**
 * Treats a trace as one segment spanning all of it.
 * <p>
 * For callers that need a trace picked but have no use for its features. DIA fragment chromatograms are the case
 * this exists for: the range a fragment is compared over comes from the precursor feature it is being tested
 * against, not from the fragment's own shape, so segmenting a fragment would be work whose result nobody reads.
 */
public class NoSegmentation implements TraceSegmentationStrategy {

    @Override
    public List<TraceSegment> detectSegments(Trace trace, double noiseLevel, double expectedPeakWidth,
                                             int[] pointsOfInterest, int[] features) {
        return List.of(new TraceSegment(trace.apex(), trace.startId(), trace.endId()));
    }
}
