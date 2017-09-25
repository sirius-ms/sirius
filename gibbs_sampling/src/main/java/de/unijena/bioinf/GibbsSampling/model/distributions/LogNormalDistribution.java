package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;

public class LogNormalDistribution implements ScoreProbabilityDistribution {
    private double logMean;
    private double logVar;
    private boolean estimateByMedian;

    public LogNormalDistribution(boolean estimateByMedian) {
        this.estimateByMedian = estimateByMedian;
        if(estimateByMedian) {
            throw new NoSuchMethodError("median estimation for log-normal not supported");
        }
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
    }

    public double toPvalue(double score) {
        return 1-this.cdf(score);
    }

    @Override
    public double toLogPvalue(double score) {
        return Math.log(toPvalue(score));
    }

    public double cdf(double value) {
        return MathUtils.cdf(Math.log(value), this.logMean, this.logVar);
    }

    public LogNormalDistribution clone() {
        return new LogNormalDistribution(this.estimateByMedian);
    }
}
