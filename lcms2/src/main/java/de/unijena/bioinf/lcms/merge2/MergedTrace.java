package de.unijena.bioinf.lcms.merge2;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.Locale;

public class MergedTrace {

    @Getter
    private int uid;
    @Getter
    private double[] mz;
    @Getter
    private double[] ints;
    @Getter
    private IntArrayList sampleIds, traceIds;
    @Getter
    private int startId;
    @Getter
    private int endId;

    public MergedTrace(int uid) {
        this.uid = uid;
        this.mz = new double[0];
        this.ints = new double[0];
        this.sampleIds = new IntArrayList();
        this.traceIds = new IntArrayList();
        this.startId = -1;
        this.endId = -1;
    }

    protected MergedTrace(int uid, double[] mz, double[] ints, int[] sampleIds, int[] traceIds, int startId, int endId) {
        this.uid = uid;
        this.mz = mz;
        this.ints = ints;
        this.sampleIds = new IntArrayList(sampleIds);
        this.traceIds = new IntArrayList(traceIds);
        this.startId = startId;
        this.endId = endId;
    }

    public void finishMerging() {
        for (int k=0; k < mz.length; ++k) {
            if (ints[k]>0) mz[k] /=  ints[k];
        }
    }

    @Override
    public String toString() {
        if (mz.length==0) return "<>";
        double m=0d;
        for (int k=0; k < mz.length; ++k) {
            if (ints[k]>0) {
                m=mz[k];
                break;
            }
        }
        return String.format(Locale.US, "MergedTrace(mz = %.4f, idx = %d..%d, %d alignments)", m, startId, endId, traceIds.size());
    }

    public Trace toTrace(ProcessedSample mergedSample) {
        ScanPointMapping mapping = mergedSample.getMapping();
        int apex = 0;
        double apexIntensity = 0d;
        double avgMz = 0d;
        double count=0;
        double minMz = Double.POSITIVE_INFINITY, maxMz=Double.NEGATIVE_INFINITY;
        for (int k=0; k < ints.length; ++k) {
            if (ints[k] > apexIntensity) {
                apexIntensity = ints[k];
                apex = k;
            }
            if (ints[k]>0 && Double.isFinite(mz[k])) {
                avgMz += mz[k] * ints[k];
                count += ints[k];
                minMz = Math.min(minMz,mz[k]);
                maxMz = Math.max(maxMz,mz[k]);
            }
        }
        avgMz /= count;
        final int apexIndex = apex + startId;
        final double averageMz = avgMz;
        final double minimumMz=minMz, maximumMz=maxMz;

        return new Trace() {
            @Override
            public int startId() {
                return startId;
            }

            @Override
            public int endId() {
                return endId;
            }

            @Override
            public int apex() {
                return apexIndex;
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
                return minimumMz;
            }

            @Override
            public double maxMz() {
                return maximumMz;
            }

            @Override
            public float intensity(int index) {
                return (float)ints[index-startId];
            }

            @Override
            public int scanId(int index) {
                return mapping.getScanIdAt(index);
            }

            @Override
            public double retentionTime(int index) {
                return mapping.getRetentionTimeAt(index);
            }
        };
    }

    public void extend(int startId, int endId) {
        if (this.startId<0) {
            this.startId = startId;
            this.endId = endId;
            int n = this.endId-this.startId+1;
            this.mz = new double[n];
            this.ints = new double[n];
        } else {
            int st = Math.min(startId, this.startId);
            int ed = Math.max(endId, this.endId);
            int n = this.endId - this.startId + 1;
            int nn = ed-st+1;
            if (nn > n) {
                final double[] mzs = new double[nn];
                final double[] ints = new double[nn];
                System.arraycopy(this.mz, 0, mzs, this.startId-st, n);
                System.arraycopy(this.ints, 0, ints, this.startId-st, n);
                this.mz = mzs;
                this.ints = ints;
                this.startId = st;
                this.endId = ed;
            }
        }
    }

    public static class DataType extends CustomDataType {

        @Override
        public int getMemory(Object obj) {
            MergedTrace t = (MergedTrace)obj;
            return 8*4 + 12 + t.mz.length*16 + t.sampleIds.size()*8;
        }

        @Override
        public void write(WriteBuffer buff, Object obj) {
            MergedTrace t = (MergedTrace)obj;
            buff.putInt(t.uid);
            buff.putInt(t.startId);
            buff.putInt(t.endId);
            writeDouble(buff, t.mz);
            writeDouble(buff, t.ints);
            writeInt(buff, t.sampleIds.toIntArray());
            writeInt(buff, t.traceIds.toIntArray());
        }

        @Override
        public Object read(ByteBuffer buff) {
            int uid = buff.getInt();
            int startId = buff.getInt();
            int endId = buff.getInt();
            double[] mz = readDouble(buff);
            double[] ints = readDouble(buff);
            int[] sampleIds = readInt(buff);
            int[] traceIds = readInt(buff);
            return new MergedTrace(uid, mz, ints, sampleIds, traceIds, startId, endId);
        }
    }
}
