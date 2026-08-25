package de.unijena.bioinf.lcms.datatypes;

import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class SpectrumDatatype extends CustomDataType<SimpleSpectrum> {
    /**
     * Rounds intensities to the precision this datatype stores them at.
     * <p>
     * Masses are written as doubles and come back exactly; intensities are written as floats, which is
     * deliberate - there are gigabytes of them and a float carries an intensity perfectly well, while an m/z
     * it does not. The cost is that a spectrum has two values: the doubles it was built with, and the
     * narrower ones it is read back as. {@code MVMap.get} returns the instance that was put while its page is
     * resident and a deserialised one once it has been evicted, so which of the two a caller sees is decided by
     * memory pressure alone. That is not a rounding difference nobody can observe: narrowing is monotonic, so it
     * cannot reorder two intensities, but it can make two of them *equal*, and every "most intensive peak within
     * a window" search breaks such a tie by taking the first peak. One run then follows a different peak than the
     * next, on the same file.
     * <p>
     * So intensities are rounded once, where a spectrum is built, and what is put into the store is what comes
     * back out of it. In place and on the array we already own, because this is per peak of every scan.
     */
    public static void roundIntensitiesToStoredPrecision(double[] intensities) {
        for (int k = 0; k < intensities.length; ++k) intensities[k] = (float) intensities[k];
    }

    /** @see #roundIntensitiesToStoredPrecision(double[]) */
    public static void roundIntensitiesToStoredPrecision(de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum<?> spectrum) {
        for (int k = 0; k < spectrum.size(); ++k)
            spectrum.setIntensityAt(k, (float) spectrum.getIntensityAt(k));
    }

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
