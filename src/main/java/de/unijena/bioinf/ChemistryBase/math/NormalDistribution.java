package de.unijena.bioinf.ChemistryBase.math;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

@HasParameters
public final class NormalDistribution extends RealDistribution {
    private final static double sqrt2pi = sqrt(2*PI);
    private final double mean, var;

    public NormalDistribution(@Parameter("mean") double mean, @Parameter("variance") double var) {
        this.mean = mean;
        this.var = var;
    }

    @Override
    public double getDensity(double x) {
        return MathUtils.pdf(x, mean, var);
    }

    @Override
    public double getProbability(double begin, double end) {
        return MathUtils.cdf(begin, end, mean, var);
    }

    /**
     * equal to 1-getProbability(mu-x, mu+x)
     * Computes the probability to observe a value deviating by x from the mean of the normal distribution
     * @param x
     * @return
     */
    public double getErrorProbability(double x) {
        return MathUtils.erfc(Math.abs(mean-x)/(Math.sqrt(2*var)));
    }

    @Override
    public double getCumulativeProbability(double x) {
        return MathUtils.cdf(x, mean, var);
    }

    @Override
    public double getLogDensity(double x) {
        return -(x - mean) * (x - mean) / (2. * var) - sqrt2pi*sqrt(var);
    }

    public double getStandardDeviation() {
        return sqrt(var);
    }

    @Override
    public double getVariance() {
        return var;
    }

    @Override
    public double getMean() {
        return mean;
    }
}
