package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import gnu.trove.list.array.TDoubleArrayList;

public class ParetoDistribution implements ScoreProbabilityDistribution {
    private double threshold;
    private double alpha;
    private boolean estimateByMedian;

    public ParetoDistribution(double threshold, boolean estimateByMedian) {
        this.threshold = threshold;
        this.estimateByMedian = estimateByMedian;
    }

    public ParetoDistribution(double threshold) {
        this(threshold, false);
    }

    public void estimateDistribution(double[] exampleValues) {
        double lnThres = Math.log(this.threshold);
        int l = 0;
        double sum = 0.0D;
        TDoubleArrayList values = new TDoubleArrayList(exampleValues.length);
        double[] median = exampleValues;
        int var9 = exampleValues.length;

        for(int alphaByMedian = 0; alphaByMedian < var9; ++alphaByMedian) {
            double v = median[alphaByMedian];
            if(v >= this.threshold) {
                sum += Math.log(v) - lnThres;
                values.add(v);
                ++l;
            }
        }

        this.alpha = (double)l / sum;
        values.sort();
        double var14 = values.get(values.size() / 2);
        System.out.println("mean: " + values.sum() / (double)l + " | estimate: " + this.alpha);
        System.out.println("median: " + var14 + " | estimate: " + Math.log(2.0D) / Math.log(var14 / this.threshold));
        if(this.estimateByMedian) {
            double var13 = Math.log(2.0D) / Math.log(var14 / this.threshold);
            if(var13 < this.threshold) {
                System.out.println("median smaller than x_min: fallback to estimation by mean");
            } else {
                this.alpha = var13;
            }
        }

        System.out.println("alpha estimate " + this.alpha);
    }

    public double toPvalue(double score) {
        return this.cdf(score);
    }

    public double getMinProbability() {
        return 0.0D;
    }

    @Override
    public double getThreshold() {
        return threshold;
    }

    public double cdf(double value) {
        return value <= this.threshold?0.0D:1.0D - Math.pow(this.threshold / (value + this.threshold), this.alpha);
    }

    public ScoreProbabilityDistribution clone() {
        return new ParetoDistribution(this.threshold, this.estimateByMedian);
    }
}
