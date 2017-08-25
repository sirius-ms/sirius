package de.unijena.bioinf.GibbsSampling.model.distributions;

import gnu.trove.list.array.TDoubleArrayList;

public class ExponentialDistribution implements ScoreProbabilityDistribution {
    private final static boolean DEBUG = false;
    private double estimationThreshold;
    private double scoringThreshold;
    private double scoringThresholdFreq;
    private double lambda;
    private boolean estimateByMedian;
    private double normalizationForThreshold;

    public ExponentialDistribution(double estimationThreshold, double scoringThresholdFreq, boolean estimateByMedian) {
        this.estimationThreshold = estimationThreshold;
        this.scoringThresholdFreq = scoringThresholdFreq;
        this.estimateByMedian = estimateByMedian;
    }

    public ExponentialDistribution(double threshold) {
        this(threshold, threshold, false);
    }

    public void estimateDistribution(double[] exampleValues) {
        int l = 0;
        double sum = 0.0D;
        TDoubleArrayList values = new TDoubleArrayList(exampleValues.length);

        for(int lambdaByMedian = 0; lambdaByMedian < exampleValues.length; ++lambdaByMedian) {
            double v = exampleValues[lambdaByMedian];
            if(v >= this.estimationThreshold && v > 0.0D) {
                sum += v - this.estimationThreshold;
                values.add(v - this.estimationThreshold);
                ++l;
            }
        }

        if (values.size()==0){
            this.lambda = 1;
        }else {
            this.lambda = (double)l / sum;
            values.sort();
            double median = values.get(values.size() / 2);
            if (DEBUG) {
                System.out.println("lambda estimate " + this.lambda);
                System.out.println("mean: " + values.sum() / (double)l + " | estimate: " + this.lambda);
                System.out.println("median "+median);

            }
            if(this.estimateByMedian) {
                double var12 = Math.log(2.0D) / median;
                if(var12 < this.estimationThreshold) {
                    if (DEBUG) System.out.println("median smaller than x_min: fallback to estimation by mean");
                } else {
                    this.lambda = var12;
                    if (DEBUG) System.out.println("lambda estimate by median" + this.lambda);
                }
            }
        }


        this.scoringThreshold = -Math.log(1.0D - this.scoringThresholdFreq) / this.lambda; //this computes the quantile for scoringThresholdFreq
        this.normalizationForThreshold = 1.0D - Math.exp(-this.lambda * this.scoringThreshold);
        if (DEBUG) System.out.println("scoringThreshold " + this.scoringThreshold + " normalizationForThreshold " + this.normalizationForThreshold);
    }

    public void setLambda(double lambda){
        this.lambda = lambda;
        this.scoringThreshold = -Math.log(1.0D - this.scoringThresholdFreq) / this.lambda; //this computes the quantile for scoringThresholdFreq
        this.normalizationForThreshold = 1.0D - Math.exp(-this.lambda * this.scoringThreshold);

    }

    public double toPvalue(double score) {
        return this.cdf(score);
    }

    public double toPvalue2(double score) {
        return score*lambda;
    }

    public double getMinProbability() {
        return this.normalizationForThreshold;
    }

    public double getMinProbability2() {
        return this.scoringThreshold*lambda;
    }

    @Override
    public double getThreshold() {
        if (DEBUG) System.out.println("lambda is "+lambda+ " | threshold "+scoringThreshold);
        return scoringThreshold;
    }

    public double cdf(double value) {
        return 1.0D - Math.exp(-this.lambda * value);
    }

    public ScoreProbabilityDistribution clone() {
        return new ExponentialDistribution(this.estimationThreshold, this.scoringThreshold, this.estimateByMedian);
    }
}
