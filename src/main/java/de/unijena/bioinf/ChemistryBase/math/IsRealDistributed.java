package de.unijena.bioinf.ChemistryBase.math;

public interface IsRealDistributed extends DensityFunction {

    public double getDensity(double x);

    public double getProbability(double begin, double end);

    public double getCumulativeProbability(double x);

    public double getVariance();

    public double getMean();

}
