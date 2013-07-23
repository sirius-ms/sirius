package de.unijena.bioinf.ChemistryBase.math;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;

import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.random;

@HasParameters
public final class ParetoDistribution extends RealDistribution {

    private final double k, xmin, kdivxmin;

    public ParetoDistribution(@Parameter("k") double k, @Parameter("xmin") double xmin) {
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

    public double getMedian() {
        return xmin * pow(2, 1d/k);
    }

    public double getInverseOfCDF(double y){
        if (y<0 || y>1) return Double.NaN;
        return  xmin * pow((1-y), (-1/k));
    }

    public double nextRandom(){
        return nextRandom(xmin, Double.POSITIVE_INFINITY);
    }

    public double nextRandom(double lowerBound, double upperBound){
        double quantil1 = getCumulativeProbability(lowerBound);
        double quantil2 = getCumulativeProbability(upperBound);
        double uniformRandom = quantil1+random()*(quantil2-quantil1);
        return getInverseOfCDF(uniformRandom);
    }

    /**
     * Estimates a new distribution by the given median value but keep the xmin
     * Important: If estimated from real data, remove first all values below xmin!!!
     */
    public static ByMedianEstimatable<ParetoDistribution> getMedianEstimator(final double xmin) {
        return new EstimateByMedian(xmin);
    }

    public static ParetoDistribution learnFromData(double[] values) {
        double xmin = Double.MAX_VALUE;
        for (double v : values) xmin = min(v, xmin);
        return learnFromData(xmin, values);
    }

    public static ParetoDistribution learnFromData(double xmin, double[] values) {
        if (xmin <= 0) throw new IllegalArgumentException("xmin have to be greater than zero, but " + xmin + " is given!");
        double m = 0d;
        for (double v : values) {
            if (v <= 0) throw new IllegalArgumentException("Negative values are not allowed, as they should have probability of zero!");
            m += Math.log(v/xmin);
        }
        return new ParetoDistribution(values.length / m, xmin);
    }

    public double getQuantile(double quantile) {
        return xmin / Math.pow(1d-quantile, 1d/k);
    }

    @Override
    public String toString() {
        return "ParetoDistribution(xmin=" + xmin + ", k=" + k + ")";
    }

    @HasParameters
    public static class EstimateByMedian implements ByMedianEstimatable<ParetoDistribution> {

        @Parameter("xmin") private double xmin;

        public EstimateByMedian() {
            this(0.01);
        }

        public EstimateByMedian(double xmin) {
            this.xmin = xmin;
        }

        public double getXmin() {
            return xmin;
        }

        public void setXmin(double xmin) {
            this.xmin = xmin;
        }

        @Override
        public ParetoDistribution extimateByMedian(double median) {
            return new ParetoDistribution(Math.log(2)/Math.log(median/xmin), xmin);
        }
    }
}
