package de.unijena.bioinf.ChemistryBase.math;

import static java.lang.Math.pow;

public final class ParetoDistribution extends RealDistribution implements ByMedianEstimatable<ParetoDistribution> {

    private final double k, xmin, kdivxmin;

    public ParetoDistribution(double k, double xmin) {
        this.k = k;
        this.xmin = xmin;
        this.kdivxmin = k/xmin;
    }

    public double getK() {
        return k;
    }

    public double getXmin() {
        return xmin;
    }

    @Override
    public double getDensity(double x) {
        if (x < xmin) return 0d;
        return kdivxmin * pow(xmin / x, k + 1);
    }

    @Override
    public double getCumulativeProbability(double x) {
        if (x < xmin) return 0d;
        return 1d - pow(xmin/x, k);
    }

    @Override
    public double getVariance() {
        return k <= 2 ? Double.NEGATIVE_INFINITY : (xmin*xmin*k)/((k-1)*(k-1)*(k-2));
    }

    @Override
    public double getMean() {
        return k <= 1 ? Double.NEGATIVE_INFINITY : (xmin*k)/(k-1);
    }

    /**
     * Estimates a new distribution by the given median value but keep the xmin
     * Important: If estimated from real data, remove first all values below xmin!!!
     * @param median median of distribution.
     * @return
     */
    @Override
    public ParetoDistribution extimateByMedian(double median) {
        return new ParetoDistribution(Math.log(2)/Math.log(median/xmin), xmin);
    }
}
