package de.unijena.bioinf.GibbsSampling.model.distributions;

import gnu.trove.list.array.TDoubleArrayList;

public class ExponentialDistribution implements ScoreProbabilityDistribution {
    private final static boolean DEBUG = true;
    private double lambda;
    private boolean estimateByMedian;
//    private double normalizationForThreshold;

    public ExponentialDistribution(boolean estimateByMedian) {
        this.estimateByMedian = estimateByMedian;
    }

    public ExponentialDistribution() {
        this(false);
    }

    @Override
    public void estimateDistribution(double[] exampleValues) {
        int l = 0;
        double sum = 0.0D;
        TDoubleArrayList values = new TDoubleArrayList(exampleValues.length);

        for(int lambdaByMedian = 0; lambdaByMedian < exampleValues.length; ++lambdaByMedian) {
            double v = exampleValues[lambdaByMedian];
            //todo changed add all values !!!!
//            if(v > 0.0D) {
                sum += v;
                values.add(v);
                ++l;
//            }
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
//                double var12 = Math.log(2.0D) / median;
//                if(var12 < this.estimationThreshold) {
//                    if (DEBUG) System.out.println("median smaller than x_min: fallback to estimation by mean");
//                } else {
//                    this.lambda = var12;
//                    if (DEBUG) System.out.println("lambda estimate by median" + this.lambda);
//                }
                this.lambda = Math.log(2.0D) / median;
            }
        }


//        this.scoringThreshold = -Math.log(1.0D - this.scoringThresholdFreq) / this.lambda; //this computes the quantile for scoringThresholdFreq
//        this.normalizationForThreshold = 1.0D - Math.exp(-this.lambda * this.scoringThreshold);
//        if (DEBUG) System.out.println("scoringThreshold " + this.scoringThreshold + " normalizationForThreshold " + this.normalizationForThreshold);


        if (DEBUG) System.out.println("lambda estimate " + this.lambda);
    }

    public void setLambda(double lambda){
        this.lambda = lambda;
//        this.scoringThreshold = -Math.log(1.0D - this.scoringThresholdFreq) / this.lambda; //this computes the quantile for scoringThresholdFreq
//        this.normalizationForThreshold = 1.0D - Math.exp(-this.lambda * this.scoringThreshold);

    }

    @Override
    public double toPvalue(double score) {
        return Math.exp(-this.lambda * score);
    }

    @Override
    public double toLogPvalue(double score) {
                return -this.lambda * score;
    }

    public double cdf(double value) {
        return 1.0D - Math.exp(-this.lambda * value);
    }

    public ScoreProbabilityDistribution clone() {
        return new ExponentialDistribution(this.estimateByMedian);
    }
}
