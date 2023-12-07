package de.unijena.bioinf.lcms.trace.segmentation;

public class TraceSegment {
    public int apex, leftEdge, rightEdge;
    public TraceSegment(int apex, int leftEdge, int rightEdge) {
        this.apex = apex;
        this.leftEdge = leftEdge;
        this.rightEdge = rightEdge;
    }
}
