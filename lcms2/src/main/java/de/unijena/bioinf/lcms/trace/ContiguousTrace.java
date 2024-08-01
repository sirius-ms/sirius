package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;

import java.io.Serializable;
import java.util.Locale;

public class ContiguousTrace implements Trace, Serializable {

    protected final transient ScanPointMapping mapping;
    protected final int uid;

    protected int startId, endId, apexId;
    protected final double averageMz, minMz, maxMz;
    protected double[] mz;
    protected float[] intensity;
    protected TraceSegment[] segments;

    ContiguousTrace(ScanPointMapping mapping, int uid, int startId, int endId, int apexId, double averageMz, double minMz, double maxMz, double[] mz, float[] intensity, TraceSegment[] segments) {
        this.mapping = mapping;
        this.uid = uid;
        this.startId = startId;
        this.endId = endId;
        this.apexId = apexId;
        this.averageMz = averageMz;
        this.minMz = minMz;
        this.maxMz = maxMz;
        this.mz = mz;
        this.segments = segments;
        this.intensity = intensity;
    }

    public ContiguousTrace(ScanPointMapping mapping, int startId, int endId, double[] mz, float[] intensity) {
        this.uid = -1;
        this.mapping = mapping;
        this.startId = startId;
        this.endId = endId;
        this.mz = mz;
        this.intensity = intensity;
        this.apexId = startId+findApex(intensity);
        final double[] stats = computeMzStats(mz, intensity, intensity[this.apexId-startId]/10);
        this.averageMz = stats[0];
        this.minMz = stats[1];
        this.maxMz = stats[2];
    }

    public int getUid() {
        return uid;
    }

    @Override
    public int startId() {
        return startId;
    }

    /**
     * last id of the trace (inclusive!!!)
     * @return
     */
    @Override
    public int endId() {
        return endId;
    }

    @Override
    public int apex() {
        return apexId;
    }

    @Override
    public double mz(int index) {
        return mz[index-startId];
    }

    @Override
    public double averagedMz() {
        return averageMz;
    }

    @Override
    public double minMz() {
        return minMz;
    }

    @Override
    public double maxMz() {
        return maxMz;
    }

    @Override
    public float intensity(int index) {
        return intensity[index-startId];
    }

    @Override
    public int scanId(int index) {
        return mapping.getScanIdAt(index);
    }

    @Override
    public double retentionTime(int index) {
        return mapping.getRetentionTimeAt(index);
    }

    /**
     * Compute the weighted average m/z value and min and max mz values in the trace
     * @param mz array of masses
     * @param intensity array of intensities
     * @param threshold consider only peaks with intensity above this threshold
     * @return array of {average m/z, min m/z, max m/z}.
     */
    private static double[] computeMzStats(double[] mz, float[] intensity, float threshold) {
        double mass = 0d, minMass=Double.POSITIVE_INFINITY, maxMass = Double.NEGATIVE_INFINITY;
        double intcum = 0d;
        for (int i=0; i < intensity.length; ++i) {
            if (intensity[i] > threshold) {
                final double r = ((double) intensity[i])/threshold; // for numerical stability
                mass += mz[i]*r;
                intcum += r;
                minMass = Math.min(minMass, mz[i]);
                maxMass = Math.max(maxMass, mz[i]);
            }
        }
        return new double[]{mass / intcum, minMass, maxMass};
    }

    /**
     * @return index of highest intensity in the given array
     */
    private static int findApex(float[] intensity) {
        int i=0; float mx = intensity[0];
        for (int j=1; j < intensity.length; ++j) {
            if (intensity[j] > mx) {
                i=j;
                mx=intensity[j];
            }
        }
        return i;
    }

    protected ScanPointMapping getMapping() {
        return mapping;
    }

    int uniqueId() {
        return uid;
    }

    public ContiguousTrace withMapping(ScanPointMapping mp) {
        if (mapping==mp) return this;
        return new ContiguousTrace(mp, uid, startId, endId, apexId, averageMz, minMz, maxMz, mz, intensity, segments);
    }

    public String toString() {
        return String.format(Locale.US, "ContiguousTrace(%d...%d) apex=%d with mz=%.4f, intensity = %.1f",
                startId, endId, apexId, averageMz,apexIntensity());
    }

    public ContiguousTrace withUID(int id) {
        return new ContiguousTrace(mapping, id, startId, endId, apexId, averageMz, minMz, maxMz, mz, intensity, segments);
    }

    @Override
    public Rect rectWithIds() {
        return new Rect((float)minMz(), (float)maxMz(), startId(), endId(), averagedMz(), uid);
    }


    @Override
    public Rect rectWithRts() {
        return new Rect((float)minMz(), (float)maxMz(), (float)retentionTime(startId), (float)retentionTime(endId()), averagedMz(), uid);
    }

    public TraceSegment[] getSegments() {
        return segments;
    }

    public void setSegments(TraceSegment[] segments) {
        this.segments = segments;
    }
}
