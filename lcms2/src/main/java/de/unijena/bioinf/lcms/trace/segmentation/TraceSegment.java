package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.trace.Trace;

public class TraceSegment {
    public int apex, leftEdge, rightEdge;

    public static TraceSegment createSegmentFor(Trace trace, int leftEdge, int rightEdge) {
        float intensity = trace.intensity(leftEdge);
        int apex = leftEdge;
        for (int i=leftEdge+1; i <= rightEdge; ++i) {
            if (trace.intensity(i)>intensity) {
                apex=i;
                intensity = trace.intensity(i);
            }
        }
        return new TraceSegment(apex, leftEdge, rightEdge);
    }
    public TraceSegment(int apex, int leftEdge, int rightEdge) {
        this.apex = apex;
        this.leftEdge = leftEdge;
        this.rightEdge = rightEdge;
        if (apex > rightEdge || apex < leftEdge) {
            throw new IllegalArgumentException();
        }
    }

    public boolean overlaps(TraceSegment mergedSegment) {
        return leftEdge <= mergedSegment.rightEdge && rightEdge >= mergedSegment.leftEdge && (leftEdge < mergedSegment.rightEdge || rightEdge > mergedSegment.leftEdge);
    }
    public boolean overlaps(Trace mergedSegment) {
        return leftEdge <= mergedSegment.endId() && rightEdge >= mergedSegment.startId() && (leftEdge < mergedSegment.endId() || rightEdge > mergedSegment.startId());
    }
}
