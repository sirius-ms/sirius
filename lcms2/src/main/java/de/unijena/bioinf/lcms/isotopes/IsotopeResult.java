package de.unijena.bioinf.lcms.isotopes;

import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class IsotopeResult {
    public int charge;
    public int[] traceIds;
    public float[] isotopeIntensities;
    public float[] isotopeMz;

    IsotopeResult(int charge, int[] traceIds, float[] isotopeMz, float[] isotopeIntensities) {
        this.charge = charge;
        this.traceIds = traceIds;
        this.isotopeIntensities = isotopeIntensities;
        this.isotopeMz = isotopeMz;
    }

    public boolean hasIsotopePeaks() {
        return isotopeIntensities.length>1;
    }

    public static void writeBinary(WriteBuffer buffer, IsotopeResult i) {
        if (i==null) {
            buffer.putInt(-1);
        } else {
            buffer.putInt(i.isotopeIntensities.length);
            buffer.put((byte)i.charge);
            for (int j=0; j < i.isotopeIntensities.length; ++j) {
                buffer.putFloat(i.isotopeMz[j]);
                buffer.putFloat(i.isotopeIntensities[j]);
            }
            for (int j=0; j < i.isotopeIntensities.length; ++j) {
                buffer.putInt(i.traceIds[j]);
            }
        }
    }
    public static IsotopeResult loadBinary(ByteBuffer buffer) {
        int len = buffer.getInt();
        if (len<0) return null;
        int charge = buffer.get();
        float[] intensities = new float[len];
        float[] mz = new float[len];
        int[] ids = new int[len];
        for (int i=0; i < intensities.length; ++i) {
            mz[i]=buffer.getFloat();
            intensities[i]=buffer.getFloat();
        }
        for (int i=0; i < intensities.length; ++i) {
            ids[i]=buffer.getInt();
        }
        return new IsotopeResult(charge, ids, mz, intensities);
    }

    public int getForNominalMass(double monoisotopicMz, int k) {
        double m = monoisotopicMz+k;
        for (int i=0; i < isotopeMz.length; ++i) {
            if (Math.abs(isotopeMz[i]-m)<0.5) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasNominalMass(double monoisotopicMz, int k) {
        return getForNominalMass(monoisotopicMz,k)>=0;
    }
}
