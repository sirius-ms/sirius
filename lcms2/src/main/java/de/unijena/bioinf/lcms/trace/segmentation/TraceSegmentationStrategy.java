package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.Trace;

import java.util.List;

public interface TraceSegmentationStrategy extends ApexDetection {

    public List<TraceSegment> detectSegments(Trace trace, double noiseLevel, double expectedPeakWidth, int[] pointsOfInterest);

    public default List<TraceSegment> detectSegments(SampleStats stats, Trace trace, int[] pointsOfInterest) {
        float intensityThreshold = stats.noiseLevel(trace.apex());
        double expectedPeakWidth = stats.getExpectedPeakWidth().orElse(0d);
        return detectSegments(trace, intensityThreshold, expectedPeakWidth, pointsOfInterest);
    }

    default int[] detectMaxima(SampleStats stats, Trace trace, int[] pointsOfInterest) {
        return detectSegments(stats, trace, pointsOfInterest).stream().mapToInt(x->x.apex).sorted().toArray();
    }
}
