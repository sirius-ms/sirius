package de.unijena.bioinf.lcms.noise;

import java.util.Arrays;

public class LocalNoiseModel implements NoiseModel {

    protected final float[] noiseLevelsPerScan;
    protected final int[] scanNumbers;

    public LocalNoiseModel(float[] noiseLevelsPerScan, int[] scanNumbers) {
        this.noiseLevelsPerScan = noiseLevelsPerScan;
        this.scanNumbers = scanNumbers;
    }

    @Override
    public double getNoiseLevel(int scanNumber, double mz) {
        final int i = Arrays.binarySearch(scanNumbers, scanNumber);
        return noiseLevelsPerScan[i];
    }

    @Override
    public double getSignalLevel(int scanNumber, double mz) {
        return getNoiseLevel(scanNumber,mz) * 10d;
    }
}
