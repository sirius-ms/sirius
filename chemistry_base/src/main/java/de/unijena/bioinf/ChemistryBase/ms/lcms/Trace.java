package de.unijena.bioinf.ChemistryBase.ms.lcms;

import javax.annotation.Nonnull;

public class Trace {
    /**
     * The offset between the first array element and the first index of the trace set
     */
    private final int indexOffset;

    /**
     * the index of the array element which is the first peak in the detected feature
     * (the peaks before it are background)
     */
    private final int detectedFeatureOffset;
    private final int detectedFeatureLength;
    @Nonnull private final double[] masses;
    @Nonnull private final float[] intensities;

    public Trace(int offset, int detectorOffset, int detectedLength, double[] masses, float[] intensities) {
        if (masses==null || intensities==null) {
            throw new NullPointerException();
        }
        if (masses.length!=intensities.length) {
            throw new IllegalArgumentException("array sizes differ: " + masses.length + " vs. " + intensities.length);
        }
        this.indexOffset = offset;
        this.detectedFeatureOffset = detectorOffset;
        this.detectedFeatureLength = detectedLength;
        this.masses = masses;
        this.intensities = intensities;
    }

    public int getIndexOffset() {
        return indexOffset;
    }

    public int getDetectedFeatureOffset() {
        return detectedFeatureOffset;
    }

    public int getDetectedFeatureLength() {
        return detectedFeatureLength;
    }

    @Nonnull
    public double[] getMasses() {
        return masses;
    }

    @Nonnull
    public float[] getIntensities() {
        return intensities;
    }
}
