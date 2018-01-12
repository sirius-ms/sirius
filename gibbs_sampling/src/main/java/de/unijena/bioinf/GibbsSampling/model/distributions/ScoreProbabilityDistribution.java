package de.unijena.bioinf.GibbsSampling.model.distributions;

public interface ScoreProbabilityDistribution extends Cloneable {
    void estimateDistribution(double[] var1);

    /**
     * the p value corresponds to 1-CDF
     * @param score
     * @return
     */
    double toPvalue(double score);

    double toLogPvalue(double score);

    ScoreProbabilityDistribution clone();
}
