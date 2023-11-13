package de.unijena.bioinf.lcms.datatypes;

import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;

import java.nio.ByteBuffer;

public class SpectrumDatatype extends CustomDataType {
    @Override
    public int getMemory(Object obj) {
        return 4 + ((SimpleSpectrum)obj).size()*(12);
    }

    @Override
    public void write(WriteBuffer buff, Object obj) {
        SimpleSpectrumHack spec = new SimpleSpectrumHack((SimpleSpectrum)obj);
        buff.putVarInt(spec.size());
        writeFixedLenDouble(buff, spec.getMasses());
        writeFixedLenFloat(buff, MatrixUtils.double2float(spec.getIntensities()));
    }
    @Override
    public Object read(ByteBuffer buff) {
        int len = DataUtils.readVarInt(buff);
        final double[] masses = readFixedLenDouble(buff, len);
        final double[] intensities = MatrixUtils.float2double(readFixedLenFloat(buff, len));
        return new SimpleSpectrum(new SimpleSpectrumHack(masses, intensities));
    }

    /**
     * a dirty hack to access mass and intensities of the BaseSpectrum class. Could do this also via
     * Java Reflection, but I think that would not be better in any regards...
     */
    protected static class SimpleSpectrumHack extends SimpleSpectrum {
        public SimpleSpectrumHack(SimpleSpectrum spec) {
            super(spec);
        }

        SimpleSpectrumHack(double[] masses, double[] intensities) {
            super(masses, intensities, true);
        }

        public double[] getMasses() {
            return masses;
        }
        public double[] getIntensities() {
            return intensities;
        }
    }

}
