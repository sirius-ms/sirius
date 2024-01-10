package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.h2.mvstore.WriteBuffer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

@Builder
public class SampleStats {

    /**
     * Assigns a noise level to each spectrum in the sample
     * The noise level is the intensity where we think all peaks below it are noise
     */
    @Getter private final float[] noiseLevelPerScan;

    /**
     * Assigns a noise level to all MS/MS in the sample
     */
    @With
    private final float ms2NoiseLevel;

    @With @Getter
    private final Deviation ms1MassDeviationWithinTraces;

    @With @Getter
    private final Deviation minimumMs1MassDeviationBetweenTraces;

    public float noiseLevel(int idx) {
        return noiseLevelPerScan[idx];
    }

    public float ms2NoiseLevel() {
        return ms2NoiseLevel;
    }

    public static class DataType extends CustomDataType {

        @Override
        public int getMemory(Object obj) {
            SampleStats s = (SampleStats)(obj);
            return s.noiseLevelPerScan.length*4 + 36;
        }

        @Override
        public void write(WriteBuffer buff, Object obj) {
            SampleStats s = (SampleStats)(obj);
            buff.putFloat(s.ms2NoiseLevel);
            writeFloat(buff, s.noiseLevelPerScan);
            buff.putDouble(s.ms1MassDeviationWithinTraces.getPpm());
            buff.putDouble(s.ms1MassDeviationWithinTraces.getAbsolute());
            buff.putDouble(s.minimumMs1MassDeviationBetweenTraces.getPpm());
            buff.putDouble(s.minimumMs1MassDeviationBetweenTraces.getAbsolute());
        }

        @Override
        public Object read(ByteBuffer buff) {
            return SampleStats.builder().ms2NoiseLevel(buff.getFloat()).noiseLevelPerScan(readFloat(buff)).
                    ms1MassDeviationWithinTraces(new Deviation(buff.getDouble(), buff.getDouble())).
                    minimumMs1MassDeviationBetweenTraces(new Deviation(buff.getDouble(), buff.getDouble())).build();
        }
    }

}
