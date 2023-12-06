package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.ScanPointMapping;

import java.util.Arrays;

public class MergedTrace extends ContiguousTrace{

    public int[] numberOfMergedScanPoints;

    MergedTrace(ScanPointMapping mapping, int uid, int startId, int endId, int apexId, double averageMz, double minMz, double maxMz, double[] mz, float[] intensity) {
        this(mapping, uid, startId, endId, apexId, averageMz, minMz, maxMz, mz, intensity, new int[intensity.length]);
    }

    public MergedTrace(ScanPointMapping mapping, int uid, int startId, int endId, int apexId, double averageMz, double minMz, double maxMz, double[] mz, float[] intensity, int[] numberOfMergedScanPoints) {
        super(mapping, uid, startId, endId, apexId, averageMz, minMz, maxMz, mz, intensity);
        this.numberOfMergedScanPoints = numberOfMergedScanPoints;
    }

    MergedTrace withMapping(ScanPointMapping mp) {
        if (mapping==mp) return this;
        return new MergedTrace(mp, uid, startId, endId, apexId,  averageMz, minMz, maxMz, mz, intensity, numberOfMergedScanPoints);
    }

    MergedTrace withUID(int id) {
        return new MergedTrace(mapping, id, startId, endId, apexId,  averageMz, minMz, maxMz, mz, intensity, numberOfMergedScanPoints);
    }

    public ContiguousTrace finishMerging() {
        final double[] mz = this.mz.clone();
        final float[] ints = this.intensity.clone();
        final int maxMerge = Arrays.stream(numberOfMergedScanPoints).max().orElse(1);
        for (int k=0; k < mz.length; ++k) {
            mz[k] = (mz[k]<=0 ? 0 : mz[k]/intensity[k]);
            ints[k] /= maxMerge; // count every non-merged value as 0.
        }
        return new ContiguousTrace(mapping, startId, endId, mz, ints).withUID(uid);
    }

    public void extend(int startId, int endId) {
        if (this.startId==0 && this.endId==0) {
            this.startId = startId;
            this.endId = endId;
            this.mz = new double[endId-startId+1];
            this.intensity = new float[mz.length];
            this.numberOfMergedScanPoints = new int[intensity.length];
        } else if (startId < this.startId || endId > this.endId) {
            endId = Math.max(this.endId, endId);
            startId = Math.min(this.startId, startId);
            final int n = endId-startId+1;
            final float[] ints = new float[n];
            final double[] mzs = new double[n];
            final int[] counts = new int[n];
            int shift = this.startId - startId;
            int oldlen = this.mz.length;
            System.arraycopy(this.intensity, 0, ints, shift, oldlen);
            System.arraycopy(this.mz, 0, mzs, shift, oldlen);
            System.arraycopy(this.numberOfMergedScanPoints, 0, counts, shift, oldlen);
            this.startId = startId;
            this.endId = endId;
            this.mz = mzs;
            this.intensity = ints;
            this.numberOfMergedScanPoints = counts;
        }
    }

    public void mergePoint(int i, double mz, float intensity) {
        this.mz[i] += mz*intensity;
        this.intensity[i] += intensity;
        this.numberOfMergedScanPoints[i]++;
    }
}
