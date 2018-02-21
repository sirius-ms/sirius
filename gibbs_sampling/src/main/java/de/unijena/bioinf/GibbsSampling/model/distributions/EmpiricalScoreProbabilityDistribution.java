package de.unijena.bioinf.GibbsSampling.model.distributions;

import java.util.Arrays;

public class EmpiricalScoreProbabilityDistribution implements ScoreProbabilityDistribution {
    final double[] scores;
    final double[] pValues;

    /**
     *
     * @param scores
     * @param pValues
     */
    public EmpiricalScoreProbabilityDistribution(double[] scores, double[] pValues) {
        this.scores = scores;
        this.pValues = pValues;
    }

    public void estimateDistribution(double[] exampleValues) {
    }

    @Override
    public void setDefaultParameters() {

    }

    public double toPvalue(double score) {
        int idx = Arrays.binarySearch(this.scores, score);
        if(idx >= 0) {
            return this.pValues[idx];
        } else {
            int insertIdx = -(idx + 1);
            return insertIdx >= this.pValues.length - 1?this.pValues[this.pValues.length - 1]:(insertIdx == 0?this.pValues[0]:this.interpolate(score, insertIdx));
        }
    }

    private double interpolate(double score, int insertIdx) {
        return ((score - this.scores[insertIdx]) * this.pValues[insertIdx] + (this.scores[insertIdx + 1] - score) * this.pValues[insertIdx + 1]) / (this.scores[insertIdx + 1] - this.scores[insertIdx]);
    }

    @Override
    public double toLogPvalue(double score) {
        return Math.log(toPvalue(score));
    }

    @Override
    public double cdf(double score) {
        return 1-toPvalue(score);
    }

    public ScoreProbabilityDistribution clone() {
        return new EmpiricalScoreProbabilityDistribution(this.scores, this.pValues);
    }
}
