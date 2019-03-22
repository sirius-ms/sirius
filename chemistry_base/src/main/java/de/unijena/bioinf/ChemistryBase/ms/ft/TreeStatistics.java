package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public final class TreeStatistics implements TreeAnnotation {

    protected final double explainedIntensity;
    protected final double explainedIntensityOfExplainablePeaks;
    protected final double ratioOfExplainedPeaks;

    private final static TreeStatistics NONE = new TreeStatistics();

    public static TreeStatistics none() {
        return NONE;
    }

    protected TreeStatistics() {
        this(0d,0d,0d);
    }

    public TreeStatistics(double explainedIntensity, double explainedIntensityOfExplainablePeaks, double ratioOfExplainedPeaks) {
        this.explainedIntensity = explainedIntensity;
        this.explainedIntensityOfExplainablePeaks = explainedIntensityOfExplainablePeaks;
        this.ratioOfExplainedPeaks = ratioOfExplainedPeaks;
    }

    public double getExplainedIntensity() {
        return explainedIntensity;
    }

    public double getExplainedIntensityOfExplainablePeaks() {
        return explainedIntensityOfExplainablePeaks;
    }

    public double getRatioOfExplainedPeaks() {
        return ratioOfExplainedPeaks;
    }
}
