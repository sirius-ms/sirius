package de.unijena.bioinf.ChemistryBase.math;


import static java.lang.Math.log;

public abstract class RealDistribution implements IsRealDistributed {

    public double getLogDensity(double x) {
        return log(getDensity(x));
    }

    public double getLogProbability(double begin, double end) {
        return log(getProbability(begin, end));
    }

    public double getLogCumulativeProbability(double x) {
        return log(getCumulativeProbability(x));
    }

    @Override
    public double getProbability(double begin, double end) {
        if (end < begin) throw new IllegalArgumentException();
        if (end==begin) return 0d;
        return getCumulativeProbability(end) - getCumulativeProbability(begin);
    }

    public static RealDistribution wrap(final IsRealDistributed d) {
        return new RealDistribution(){

            @Override
            public double getDensity(double x) {
                return d.getDensity(x);
            }

            @Override
            public double getCumulativeProbability(double x) {
                return d.getCumulativeProbability(x);
            }

            @Override
            public double getVariance() {
                return d.getVariance();
            }

            @Override
            public double getMean() {
                return d.getMean();
            }
        };
    }
}
