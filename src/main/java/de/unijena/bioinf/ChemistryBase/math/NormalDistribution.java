package de.unijena.bioinf.ChemistryBase.math;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

public final class NormalDistribution extends RealDistribution {
    private final static double sqrt2pi = sqrt(2*PI);
    private final double mean, var;

    public NormalDistribution(double mean, double var) {
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

    @Override
    public double getCumulativeProbability(double x) {
        return MathUtils.cdf(x, mean, var);
    }

    @Override
    public double getLogDensity(double x) {
        return -(x - mean) * (x - mean) / (2. * var) - sqrt2pi*sqrt(var);
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
