package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public final class TreeStatistics implements TreeAnnotation {

    /**
     * an intensity ratio: intensity summed over all peaks explained by the tree normalized by total intensity. ignores FT root and artificial peaks
     */
    protected final double explainedIntensity;

    /**
     * an intensity ratio: intensity summed over all peaks explained by the tree normalized by total intensity of peaks with MF decomposition. ignores FT root and artificial peaks
     */
    protected final double explainedIntensityOfExplainablePeaks;

    /**
     * ratio of explained and total number of peaks. ignores FT root and artificial peaks
     */
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
