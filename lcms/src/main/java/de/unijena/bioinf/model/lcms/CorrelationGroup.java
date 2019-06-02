package de.unijena.bioinf.model.lcms;

public class CorrelationGroup {
    protected ChromatographicPeak left, right;
    protected ChromatographicPeak.Segment leftSegment, rightSegment;
    protected int numberOfCorrelatedPeaks;
    protected double correlation;

    public CorrelationGroup(ChromatographicPeak left, ChromatographicPeak right, ChromatographicPeak.Segment leftSegment, ChromatographicPeak.Segment rightSegment, int numberOfCorrelatedPeaks, double correlation) {
        this.left = left;
        this.right = right;
        this.leftSegment = leftSegment;
        this.rightSegment = rightSegment;
        this.correlation = correlation;
        this.numberOfCorrelatedPeaks = numberOfCorrelatedPeaks;
    }

    public ChromatographicPeak getLeft() {
        return left;
    }

    public ChromatographicPeak getRight() {
        return right;
    }

    public ChromatographicPeak.Segment getLeftSegment() {
        return leftSegment;
    }

    public ChromatographicPeak.Segment getRightSegment() {
        return rightSegment;
    }

    public int getNumberOfCorrelatedPeaks() {
        return numberOfCorrelatedPeaks;
    }

    public double getCorrelation() {
        return correlation;
    }

    public CorrelationGroup invert() {
        return new CorrelationGroup(right,left,rightSegment,leftSegment,numberOfCorrelatedPeaks, correlation);
    }

    @Override
    public String toString() {
        return numberOfCorrelatedPeaks + " peaks with correlation = " + correlation;
    }
}
