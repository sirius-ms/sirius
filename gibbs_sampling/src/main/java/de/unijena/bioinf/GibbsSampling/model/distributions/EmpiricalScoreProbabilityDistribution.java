package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import java.util.Arrays;

public class EmpiricalScoreProbabilityDistribution implements ScoreProbabilityDistribution {
    final double[] scores;
    final double[] probabilities;

    public EmpiricalScoreProbabilityDistribution(double[] scores, double[] probabilities) {
        this.scores = scores;
        this.probabilities = probabilities;
    }

    public void estimateDistribution(double[] exampleValues) {
    }

    public double toPvalue(double score) {
        int idx = Arrays.binarySearch(this.scores, score);
        if(idx >= 0) {
            return this.probabilities[idx];
        } else {
            int insertIdx = -(idx + 1);
            return insertIdx >= this.probabilities.length - 1?this.probabilities[this.probabilities.length - 1]:(insertIdx == 0?this.probabilities[0]:this.interpolate(score, insertIdx));
        }
    }

    private double interpolate(double score, int insertIdx) {
        return ((score - this.scores[insertIdx]) * this.probabilities[insertIdx] + (this.scores[insertIdx + 1] - score) * this.probabilities[insertIdx + 1]) / (this.scores[insertIdx + 1] - this.scores[insertIdx]);
    }

    public double getThreshold() {
        return 0.0D;
    }

    public ScoreProbabilityDistribution clone() {
        return new EmpiricalScoreProbabilityDistribution(this.scores, this.probabilities);
    }
}
