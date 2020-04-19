package de.unijena.bioinf.ChemistryBase.ms.lcms;

import javax.annotation.Nonnull;

public class Trace {

    private final int offset, detectorOffset, detectedLength;
    @Nonnull private final double[] masses;
    @Nonnull private final float[] intensities;

    public Trace(int offset, int detectorOffset, int detectedLength, double[] masses, float[] intensities) {
        if (masses==null || intensities==null) {
            throw new NullPointerException();
        }
        if (masses.length!=intensities.length) {
            throw new IllegalArgumentException("array sizes differ: " + masses.length + " vs. " + intensities.length);
        }
        this.offset = offset;
        this.detectorOffset = detectorOffset;
        this.detectedLength = detectedLength;
        this.masses = masses;
        this.intensities = intensities;
    }
}
