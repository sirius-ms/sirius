package de.unijena.bioinf.lcms.align2;

import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import de.unijena.bioinf.lcms.trace.Rect;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Mass of Interest
 */
public class MoI {

    @Getter private final Rect rect;
    @Getter @With private final double retentionTime;
    @Getter private final int scanId;
    @Getter private final int sampleIdx;
    @Getter @Setter @With private long uid;

    @Getter @Setter
    private float intensity;

    @Getter @Setter
    private float confidence;
    public MoI(Rect rect, int scanId, double retentionTime, float intensity, int sampleIdx) {
        this.rect = rect;
        this.scanId = scanId;
        this.retentionTime = retentionTime;
        this.intensity = intensity;
        this.sampleIdx = sampleIdx;
        this.uid = -1;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "MoI(mz = %.4f, rt = %.1f)", rect.avgMz, retentionTime);
    }

    protected MoI(Rect rect, double retentionTime, int scanId, int sampleIdx, long uid, float intensity, float confidence) {
        this.rect = rect;
        this.retentionTime = retentionTime;
        this.scanId = scanId;
        this.sampleIdx = sampleIdx;
        this.uid = uid;
        this.intensity = intensity;
        this.confidence = confidence;
    }

    public int getTraceId() {
        return rect.id;
    }

    public double getMz() {
        return rect.avgMz;
    }

    public static class DataType extends CustomDataType {

        @Override
        public int getMemory(Object obj) {
            int base = 60;
            if (obj instanceof AlignedMoI) {
                base *= (1+((AlignedMoI) obj).getAligned().length);
                base += 8;
            }
            return base;
        }

        @Override
        public void write(WriteBuffer buff, Object obj) {
            MoI m = (MoI)obj;
            buff.putLong(m.uid);
            writeFixedLenFloat(buff, m.rect.toArray());
            buff.putDouble(m.rect.avgMz);
            buff.putInt(m.rect.id);
            buff.putDouble(m.retentionTime);
            buff.putFloat(m.intensity);
            buff.putFloat(m.confidence);
            buff.putInt(m.scanId);
            buff.putInt(m.sampleIdx);
            if (m instanceof AlignedMoI) {
                AlignedMoI n = (AlignedMoI) m;
                buff.putInt(n.getAligned().length);
                for (MoI a : n.getAligned()) {
                    write(buff, a);
                }
            } else {
                buff.putInt(-1);
            }
        }

        @Override
        public Object read(ByteBuffer buff) {
            long uid = buff.getLong();
            float[] rect = readFixedLenFloat(buff, 4);
            double avgMz = buff.getDouble();
            int rectId = buff.getInt();
            double rt = buff.getDouble();
            float intensity = buff.getFloat();
            float confidence = buff.getFloat();
            int scanId = buff.getInt();
            int sampleIdx = buff.getInt();
            int aligns = buff.getInt();
            if (aligns >= 0) {
                MoI[] parts = new MoI[aligns];
                for (int k=0; k < aligns; ++k) parts[k] = (MoI)read(buff);
                return new AlignedMoI(new Rect(rect[0],rect[1],rect[2],rect[3], avgMz, rectId), rt, scanId, sampleIdx, uid, intensity, confidence, parts);
            } else {
                return new MoI(new Rect(rect[0],rect[1],rect[2],rect[3], avgMz, rectId), rt, scanId, sampleIdx, uid, intensity, confidence);
            }
        }
    }

}
