package de.unijena.bioinf.model.lcms;

public interface NoiseModel {

    /**
     * Values below this intensity are most likely noise
     */
    public double getNoiseLevel(int scanNumber, double mz);

    /**
     * Values above this intensity might be signal.
     */
    public double getSignalLevel(int scanNumber, double mz);

}
