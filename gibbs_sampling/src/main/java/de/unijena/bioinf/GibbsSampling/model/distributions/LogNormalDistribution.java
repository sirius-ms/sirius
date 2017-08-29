package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;

public class LogNormalDistribution implements ScoreProbabilityDistribution {
    private double threshold;
    private double thresholdFreq;
    private double logMean;
    private double logVar;
    private double normalizationForThreshold;
    private boolean estimateByMedian;

    public LogNormalDistribution(double thresholdFreq, boolean estimateByMedian) {
        this.thresholdFreq = thresholdFreq;
        this.estimateByMedian = estimateByMedian;
        if(estimateByMedian) {
            throw new NoSuchMethodError("median estimation for log-normal not supported");
        }
    }

    public LogNormalDistribution(double threshold) {
        this(threshold, false);
    }

    public void estimateDistribution(double[] exampleValues) {
        int l = 0;
        double logMean = 0.0D;
        double logVar = 0.0D;
        double[] var7 = exampleValues;
        int var8 = exampleValues.length;

        int var9;
        double v;
        for(var9 = 0; var9 < var8; ++var9) {
            v = var7[var9];
            if(v > 0.0D) {
                logMean += Math.log(v);
                ++l;
            }
        }

        logMean /= (double)l;
        var7 = exampleValues;
        var8 = exampleValues.length;

        for(var9 = 0; var9 < var8; ++var9) {
            v = var7[var9];
            if(v > 0.0D) {
                double s = Math.log(v) - logMean;
                logVar += s * s;
            }
        }

        logVar /= (double)(l - 1);
        System.out.println("logmean " + logMean + " logvar " + logVar);
        this.logMean = logMean;
        this.logVar = logVar;
        this.threshold = this.tryThreshold();
        this.normalizationForThreshold = MathUtils.cdf(Math.log(this.threshold), logMean, logVar);
        System.out.println("norm " + this.normalizationForThreshold);
    }

    private double tryThreshold() {
        double t = 0.0D;

        for(double step = 0.001D; MathUtils.cdf(Math.log(t), this.logMean, this.logVar) < this.thresholdFreq; t += step) {
            ;
        }

        System.out.println("change threshold " + this.thresholdFreq + " to " + t);
        return t;
    }

    public double toPvalue(double score) {
        return this.cdf(score);
    }

    public double getMinProbability() {
        return this.normalizationForThreshold;
    }

    @Override
    public double getThreshold() {
        return threshold;
    }

    public double cdf(double value) {
        return MathUtils.cdf(Math.log(value), this.logMean, this.logVar);
    }

    public LogNormalDistribution clone() {
        return new LogNormalDistribution(this.threshold, this.estimateByMedian);
    }
}
