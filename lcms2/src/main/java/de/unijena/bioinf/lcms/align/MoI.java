package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import de.unijena.bioinf.lcms.isotopes.IsotopeResult;
import de.unijena.bioinf.lcms.trace.Rect;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.h2.mvstore.WriteBuffer;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Mass of Interest
 */
public class MoI {

    private static final byte ISOTOPE_FLAG = 1, MULTIPLE_CHARGED = 2;

    @Getter private final Rect rect;
    @Getter @With private final double retentionTime;
    @Getter private final int scanId;
    @Getter private final int sampleIdx;
    @Getter @Setter @With private long uid;

    @Getter @Setter
    private float intensity;

    @Getter @Setter
    private float confidence;

    @Getter @Setter @Nullable
    private IsotopeResult isotopes;

    protected byte state;

    public MoI(Rect rect, int scanId, double retentionTime, float intensity, int sampleIdx) {
        this.rect = rect;
        this.scanId = scanId;
        this.retentionTime = retentionTime;
        this.intensity = intensity;
        this.sampleIdx = sampleIdx;
        this.uid = -1;
        this.isotopes = null;
        this.state = 0;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "MoI(mz = %.4f, rt = %.1f)", rect.avgMz, retentionTime);
    }

    protected MoI(Rect rect, double retentionTime, int scanId, int sampleIdx, long uid, float intensity, float confidence, IsotopeResult isotopes, byte state) {
        this.rect = rect;
        this.retentionTime = retentionTime;
        this.scanId = scanId;
        this.sampleIdx = sampleIdx;
        this.uid = uid;
        this.intensity = intensity;
        this.confidence = confidence;
        this.isotopes = isotopes;
        this.state = state;
    }

    public boolean isIsotopePeak() {
        return (this.state & ISOTOPE_FLAG) != 0;
    }

    public boolean isMultiCharged() {
        return (this.state & MULTIPLE_CHARGED) != 0;
    }

    public void setIsotopePeakFlag(boolean value) {
        if (value) this.state |= ISOTOPE_FLAG;
        else this.state &= ~ISOTOPE_FLAG;
    }
    public void setMultiChargeFlag(boolean value) {
        if (value) this.state |= MULTIPLE_CHARGED;
        else this.state &= ~MULTIPLE_CHARGED;
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
            buff.put(m.state);
            if (m instanceof AlignedMoI) {
                AlignedMoI n = (AlignedMoI) m;
                buff.putInt(n.getAligned().length);
                for (MoI a : n.getAligned()) {
                    write(buff, a);
                }
            } else {
                buff.putInt(-1);
                IsotopeResult.writeBinary(buff, m.isotopes);
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
            byte state = buff.get();
            int aligns = buff.getInt();
            if (aligns >= 0) {
                MoI[] parts = new MoI[aligns];
                for (int k=0; k < aligns; ++k) parts[k] = (MoI)read(buff);
                return new AlignedMoI(new Rect(rect[0],rect[1],rect[2],rect[3], avgMz, rectId), rt, scanId, sampleIdx, uid, intensity, confidence, parts, state);
            } else {
                IsotopeResult is = IsotopeResult.loadBinary(buff);
                return new MoI(new Rect(rect[0],rect[1],rect[2],rect[3], avgMz, rectId), rt, scanId, sampleIdx, uid, intensity, confidence, is, state);
            }
        }
    }
    public boolean hasIsotopes() {
        return isotopes!=null && isotopes.isotopeIntensities.length>1;
    }

}
