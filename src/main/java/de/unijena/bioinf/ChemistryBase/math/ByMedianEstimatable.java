package de.unijena.bioinf.ChemistryBase.math;

public interface ByMedianEstimatable<T extends IsRealDistributed> extends IsRealDistributed {

    public T extimateByMedian(double median);



}
