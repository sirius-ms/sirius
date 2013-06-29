package de.unijena.bioinf.ChemistryBase.math;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.ImmutableParameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.ParameterHelper;

import static java.lang.Math.exp;

@HasParameters
public final class ExponentialDistribution extends RealDistribution implements ByMedianEstimatable<ExponentialDistribution> {

    @Parameter
    private final double lambda;

    public static ExponentialDistribution fromLambda(double lambda) {
        return new ExponentialDistribution(lambda);
    }

    public static ExponentialDistribution fromMean(double mean) {
        return new ExponentialDistribution(1/mean);
    }

    ExponentialDistribution(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public double getDensity(double x) {
        return x < 0d ? 0d : lambda * exp(-lambda*x);
    }

    @Override
    public double getCumulativeProbability(double x) {
        if (x < 0) return 0;
        return 1-exp(-lambda*x);
    }

    @Override
    public double getInverseLogCumulativeProbability(double x) {
        if (x < 0) return Double.NEGATIVE_INFINITY;
        return -lambda*x;
    }

    @Override
    public double getVariance() {
        return 1d/(lambda*lambda);
    }

    @Override
    public double getMean() {
        return 1d/lambda;
    }

    @Override
    public ExponentialDistribution extimateByMedian(double median) {
        return ExponentialDistribution.fromLambda(Math.log(2)/median);
    }
}
