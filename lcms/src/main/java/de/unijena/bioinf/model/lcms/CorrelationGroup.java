package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class CorrelationGroup {
    protected ChromatographicPeak left, right;
    protected ChromatographicPeak.Segment leftSegment, rightSegment;
    protected int start, end;
    protected double correlation, kl;

    protected String annotation;

    // for adduct types
    protected PrecursorIonType leftType, rightType;

    public CorrelationGroup(ChromatographicPeak left, ChromatographicPeak right, ChromatographicPeak.Segment leftSegment, ChromatographicPeak.Segment rightSegment, int start, int end, double correlation, double kl) {
        this.left = left;
        this.right = right;
        this.leftSegment = leftSegment;
        this.rightSegment = rightSegment;
        this.correlation = correlation;
        this.kl = kl;
        this.start = start;
        this.end = end;
    }

    public double getKullbackLeibler() {
        return kl;
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
        return end-start+1;
    }

    public double getCorrelation() {
        return correlation;
    }

    public CorrelationGroup invert() {
        return new CorrelationGroup(right,left,rightSegment,leftSegment,start,end, correlation,kl);
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public PrecursorIonType getLeftType() {
        return leftType;
    }

    public void setLeftType(PrecursorIonType leftType) {
        this.leftType = leftType;
    }

    public PrecursorIonType getRightType() {
        return rightType;
    }

    public void setRightType(PrecursorIonType rightType) {
        this.rightType = rightType;
    }

    @Override
    public String toString() {
        return getNumberOfCorrelatedPeaks() + " peaks with correlation = " + correlation;
    }
}
