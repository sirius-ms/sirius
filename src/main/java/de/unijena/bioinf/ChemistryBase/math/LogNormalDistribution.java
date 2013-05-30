package de.unijena.bioinf.ChemistryBase.math;
import static java.lang.Math.*;

public class LogNormalDistribution extends RealDistribution {

    public static final double SQRT2PI = sqrt(2 * PI);
    private final double mean, var, sd;

    public LogNormalDistribution(double mean, double var) {
        this.mean = mean;
        this.var = var;
        this.sd = sqrt(var);
    }

    @Override
    public double getDensity(double x) {
        return 1/(SQRT2PI *sd*x) * exp(-pow(log(x)-mean, 2)/(2*var));
    }

    @Override
    public double getCumulativeProbability(double x) {
        return MathUtils.cdf(log(x), mean, var);
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
