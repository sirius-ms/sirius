package de.unijena.bioinf.lcms.noise;

/**
 * Sample datapoints from scans.
 * Assume that noise is same level everywhere
 */
public class GlobalNoiseModel implements NoiseModel {

    private final double noiseLevel, signalLevel;

    public GlobalNoiseModel(double noiseLevel, double signalLevel) {
        this.noiseLevel = noiseLevel;
        this.signalLevel = signalLevel;
    }

    @Override
    public double getNoiseLevel(int scanNumber, double mz) {
        return noiseLevel;
    }

    @Override
    public double getSignalLevel(int scanNumber, double mz) {
        return signalLevel;
    }

    @Override
    public String toString() {
        return "GlobalNoiseModel{" +
                "noiseLevel=" + noiseLevel +
                ", signalLevel=" + signalLevel +
                '}';
    }
}
