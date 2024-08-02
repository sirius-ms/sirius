package de.unijena.bioinf.lcms.datatypes;

import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class SpectrumDatatype extends CustomDataType<SimpleSpectrum> {
    @Override
    public int getMemory(SimpleSpectrum obj) {
        return 4 + obj.size()*(12);
    }

    @Override
    public void write(WriteBuffer buff, SimpleSpectrum obj) {
        SimpleSpectrumHack spec = new SimpleSpectrumHack(obj);
        buff.putVarInt(spec.size());
        writeFixedLenDouble(buff, spec.getMasses());
        writeFixedLenFloat(buff, MatrixUtils.double2float(spec.getIntensities()));
    }
    @Override
    public SimpleSpectrum read(ByteBuffer buff) {
        int len = DataUtils.readVarInt(buff);
        final double[] masses = readFixedLenDouble(buff, len);
        final double[] intensities = MatrixUtils.float2double(readFixedLenFloat(buff, len));
        return new SimpleSpectrum(new SimpleSpectrumHack(masses, intensities));
    }

    @Override
    public SimpleSpectrum[] createStorage(int i) {
        return new SimpleSpectrum[i];
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
