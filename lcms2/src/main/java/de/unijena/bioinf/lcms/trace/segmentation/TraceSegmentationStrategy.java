package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.Trace;

import java.util.List;

public interface TraceSegmentationStrategy extends ApexDetection {

    /**
     * @param trace trace to search in
     * @param noiseLevel determines which peaks to keep
     * @param expectedPeakWidth determines when to split peaks
     * @param pointsOfInterest usually MS/MS but also other scan points that HAVE TO BE included in any peak. Can be kept empty.
     * @param features scan points that should be a feature. Two features should never share the same peak. Can be kept empty.
     * @return
     */
    public List<TraceSegment> detectSegments(Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest, int[] features);

    default List<TraceSegment> detectSegments(SampleStats stats, Trace trace, int[] pointsOfInterest) {
        return detectSegments(stats,trace,pointsOfInterest,new int[0]);
    }

    public default List<TraceSegment> detectSegments(SampleStats stats, Trace trace, int[] pointsOfInterest, int[] features) {
        float intensityThreshold = stats.noiseLevel(trace.apex());
        double expectedPeakWidth = stats.getExpectedPeakWidth().orElse(0d);
        return detectSegments(trace, intensityThreshold, expectedPeakWidth, pointsOfInterest, features);
    }

    default List<TraceSegment> detectSegments(Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest) {
        return detectSegments(trace,noiseLevel,expectedPeakWidth,pointsOfInterest,new int[0]);
    }
    default int[] detectMaxima(SampleStats stats, Trace trace, int[] pointsOfInterest) {
        return detectSegments(stats, trace, pointsOfInterest, new int[0]).stream().mapToInt(x->x.apex).sorted().toArray();
    }
}
