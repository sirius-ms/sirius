package de.unijena.bioinf.ChemistryBase.ms.lcms;

import javax.annotation.Nonnull;

/**
 * The LCMSSubtrace is a mass trace of a compound. It consists of the scan ids and retention times
 * the compound is eluting at, as well as all scans before and afterwards which have the same mass
 * and, therefore, were separated from the compound by some feature detection algorithm.
 * The reason we want those points before and after the compound is that this algorithm might
 * have done failures. By visualizing the full trace, the user can pin-point such failures.
 *
 * We also add all mass traces of the adducts, isotopes, and in-source fragments. Those traces
 * have to be contained within the mass trace of the compound.
 *
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
