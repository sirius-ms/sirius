package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.GibbsSampling.model.GibbsMFCorrectionNetwork;
import gnu.trove.list.array.TDoubleArrayList;

public class ExponentialDistribution implements ScoreProbabilityDistribution {
    private double lambda;
    private boolean estimateByMedian;
    private final double DEFAULT_LAMBDA = 7d;


    public ExponentialDistribution(double lambda) {
        this.lambda = lambda;
    }

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
            if(v > 0.0D) {
                sum += v;
                values.add(v);
                ++l;
            }
        }

        if (values.size()<10){
            System.out.println("warning: Problem estimating score distribution. Too few examples. Using default values.");
            this.lambda = DEFAULT_LAMBDA;
        }else {
            this.lambda = (double)l / sum;
            values.sort();
            double median = values.get(values.size() / 2);
            if (GibbsMFCorrectionNetwork.DEBUG) {
                System.out.println("lambda estimate " + this.lambda);
                System.out.println("mean: " + values.sum() / (double)l + " | estimate: " + this.lambda);
                System.out.println("median "+median);

            }
            if(this.estimateByMedian) {
                this.lambda = Math.log(2.0D) / median;
            }
        }

        if (Double.isNaN(lambda) || Double.isInfinite(lambda)){
            System.out.println("warning: Problem estimating score distribution. Using default values.");
            lambda = DEFAULT_LAMBDA;
        }


        if (GibbsMFCorrectionNetwork.DEBUG) System.out.println("lambda estimate " + this.lambda);
    }

    public void setLambda(double lambda){
        this.lambda = lambda;
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
