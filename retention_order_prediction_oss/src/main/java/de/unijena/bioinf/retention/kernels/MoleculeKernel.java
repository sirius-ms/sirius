package de.unijena.bioinf.retention.kernels;

import de.unijena.bioinf.retention.PredictableCompound;

public interface MoleculeKernel<T> {

    public T prepare(PredictableCompound compound);

    public double compute(PredictableCompound left, PredictableCompound right, T preparedLeft, T preparedRight);

}
