package de.unijena.bioinf.GibbsSampling.model.distributions;

public interface ScoreProbabilityDistribution extends Cloneable {
    void estimateDistribution(double[] var1);

    /*
    setting default parameters (might be necessary if not enough data is available to estimate parameters)
     */
    void setDefaultParameters();

    /**
     * the p value corresponds to 1-CDF
     * @param score
     * @return
     */
    double toPvalue(double score);

    double toLogPvalue(double score);

    double cdf(double score);

    ScoreProbabilityDistribution clone();
}
