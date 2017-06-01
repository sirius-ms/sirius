package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;
import gnu.trove.list.array.TDoubleArrayList;

public class ExponentialDistribution implements ScoreProbabilityDistribution {
    private double estimationThreshold;
    private double scoringThreshold;
    private double lambda;
    private boolean estimateByMedian;
    private double normalizationForThreshold;

    public ExponentialDistribution(double estimationThreshold, double scoringThreshold, boolean estimateByMedian) {
        this.estimationThreshold = estimationThreshold;
        this.scoringThreshold = scoringThreshold;
        this.estimateByMedian = estimateByMedian;
    }

    public ExponentialDistribution(double threshold) {
        this(threshold, threshold, false);
    }

    public void estimateDistribution(double[] exampleValues) {
        int l = 0;
        double sum = 0.0D;
        TDoubleArrayList values = new TDoubleArrayList(exampleValues.length);
        double[] median = exampleValues;
        int var7 = exampleValues.length;

        for(int lambdaByMedian = 0; lambdaByMedian < var7; ++lambdaByMedian) {
            double v = median[lambdaByMedian];
            if(v >= this.estimationThreshold && v > 0.0D) {
                sum += v - this.estimationThreshold;
                values.add(v - this.estimationThreshold);
                ++l;
            }
        }

        this.lambda = (double)l / sum;
//        System.out.println("lambda estimate " + this.lambda);
        values.sort();
        double var11 = values.get(values.size() / 2);
//        System.out.println("mean: " + values.sum() / (double)l + " | estimate: " + this.lambda);
//        System.out.println("median: " + var11 + " | estimate: " + Math.log(2.0D) / var11);
        if(this.estimateByMedian) {
            double var12 = Math.log(2.0D) / var11;
            if(var12 < this.estimationThreshold) {
                System.out.println("median smaller than x_min: fallback to estimation by mean");
            } else {
                this.lambda = var12;
            }
        }

        this.scoringThreshold = -Math.log(1.0D - this.scoringThreshold) / this.lambda;
        this.normalizationForThreshold = 1.0D - Math.exp(-this.lambda * this.scoringThreshold);
//        System.out.println("scoringThreshold " + this.scoringThreshold + " normalizationForThreshold " + this.normalizationForThreshold);
    }

    public double toPvalue(double score) {
        return this.cdf(score);
    }

    public double getMinProbability() {
        return this.normalizationForThreshold;
    }

    @Override
    public double getThreshold() {
        return scoringThreshold;
    }

    public double cdf(double value) {
        return 1.0D - Math.exp(-this.lambda * value);
    }

    public ScoreProbabilityDistribution clone() {
        return new ExponentialDistribution(this.estimationThreshold, this.scoringThreshold, this.estimateByMedian);
    }
}
