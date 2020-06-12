package de.unijena.bioinf.ChemistryBase.ms.lcms;

import javax.annotation.Nonnull;

/**
 * Each compound is assigned to an CoelutingTraceset PER SAMPLE. The CoelutingTraceset is
 * the collection of all mass traces associated to the compound and its correlated ions (adducts,
 * isotopes, in-source fragments)
 */
public class CoelutingTraceSet {

    @Nonnull protected final String sampleName;
    @Nonnull protected final MsDataSourceReference sampleRef;
    @Nonnull protected final CompoundTrace ionTrace;

    @Nonnull  protected final long[] retentionTimes;
    @Nonnull  protected final int[] scanIds; // the INDEX of the spectrum
    @Nonnull protected final float[] noiseLevels;

    public CoelutingTraceSet(@Nonnull String sampleName, @Nonnull MsDataSourceReference sampleRef, @Nonnull CompoundTrace trace, @Nonnull long[] retentionTimes, @Nonnull int[] scanIds, @Nonnull float[] noiselevels) {
        this.sampleName = sampleName;
        this.sampleRef = sampleRef;
        this.ionTrace = trace;
        this.retentionTimes = retentionTimes;
        this.scanIds = scanIds;
        this.noiseLevels = noiselevels;
    }

    @Nonnull
    public String getSampleName() {
        return sampleName;
    }

    @Nonnull
    public MsDataSourceReference getSampleRef() {
        return sampleRef;
    }

    @Nonnull
    public CompoundTrace getIonTrace() {
        return ionTrace;
    }

    @Nonnull
    public long[] getRetentionTimes() {
        return retentionTimes;
    }

    @Nonnull
    public int[] getScanIds() {
        return scanIds;
    }

    @Nonnull
    public float[] getNoiseLevels() {
        return noiseLevels;
    }
}
