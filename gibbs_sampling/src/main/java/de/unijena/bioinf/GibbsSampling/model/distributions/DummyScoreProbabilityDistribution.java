package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;

public class DummyScoreProbabilityDistribution implements ScoreProbabilityDistribution {
    public DummyScoreProbabilityDistribution() {
    }

    public void estimateDistribution(double[] exampleValues) {
    }

    public double toPvalue(double score) {
        return score;
    }

    public double getThreshold() {
        return 0.0D;
    }

    public ScoreProbabilityDistribution clone() {
        return new DummyScoreProbabilityDistribution();
    }
}
