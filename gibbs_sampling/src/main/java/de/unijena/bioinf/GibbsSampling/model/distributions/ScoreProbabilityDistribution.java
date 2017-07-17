package de.unijena.bioinf.GibbsSampling.model.distributions;

public interface ScoreProbabilityDistribution extends Cloneable {
    void estimateDistribution(double[] var1);

    double toPvalue(double var1);

    double getMinProbability();

    double getThreshold();

    ScoreProbabilityDistribution clone();
}
