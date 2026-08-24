package de.unijena.bioinf.lcms.datatypes;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.h2.mvstore.WriteBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A spectrum has to come back from the store as the spectrum that went in.
 * <p>
 * {@link SimpleSpectrum} declares itself an {@code OrderedSpectrum}, so every lookup in the codebase reaches it by
 * binary search, and the datatype rebuilds it with the "do not copy and do not order" constructor - the point being
 * that a spectrum known to be sorted need not be sorted again. Nothing verifies that, so if the round trip disturbs
 * the arrays the spectrum keeps claiming to be ordered and every binary search over it silently returns the wrong
 * answer, or none: on a real run {@code mostIntensivePeakWithin} reported no peak within eight ppm while a linear scan
 * over the same spectrum found one 3.7 mDa away.
 */
public class SpectrumDatatypeTest {

    private static SimpleSpectrum roundTrip(SimpleSpectrum in) {
        final SpectrumDatatype type = new SpectrumDatatype();
        final WriteBuffer out = new WriteBuffer();
        type.write(out, in);
        final ByteBuffer bytes = out.getBuffer();
        bytes.flip();
        return type.read(bytes);
    }

    /** realistic fragment spectrum: masses ascending, intensities in no particular order */
    private static SimpleSpectrum spectrumOf(int size) {
        final double[] mz = new double[size];
        final double[] intensity = new double[size];
        for (int k = 0; k < size; ++k) {
            mz[k] = 100d + k * 0.7134 + (k % 5) * 1e-4;
            intensity[k] = 100d + ((k * 37) % 91) * 13d;
        }
        return new SimpleSpectrum(mz, intensity);
    }

    /**
     * Several spectra written into one buffer and read back in order, which is how a page really holds them.
     * <p>
     * The first value of a page starts at offset zero and every later one does not, so a datatype that is only ever
     * exercised from zero can be wrong for all but the first value in a page. These helpers mix {@code asDoubleBuffer}
     * views, which start at the buffer's current position, with manual position arithmetic and a capacity check that
     * computes an absolute offset - so the offset is exactly what has to be varied.
     */
    @Test
    public void severalSpectraShareOneBufferAsAPageDoes() {
        final SpectrumDatatype type = new SpectrumDatatype();
        final int[] sizes = {111, 116, 107, 7, 1000, 3};
        final SimpleSpectrum[] in = new SimpleSpectrum[sizes.length];
        final WriteBuffer out = new WriteBuffer();
        for (int k = 0; k < sizes.length; ++k) {
            in[k] = spectrumOf(sizes[k]);
            type.write(out, in[k]);
        }
        final ByteBuffer bytes = out.getBuffer();
        bytes.flip();
        for (int k = 0; k < sizes.length; ++k) {
            final SimpleSpectrum back = type.read(bytes);
            assertEquals(in[k].size(), back.size(), "size of spectrum " + k);
            for (int q = 0; q < in[k].size(); ++q) {
                assertEquals(in[k].getMzAt(q), back.getMzAt(q), 0d,
                        "mass " + q + " of spectrum " + k + " (size " + sizes[k] + ")");
            }
            for (int q = 1; q < back.size(); ++q) {
                assertTrue(back.getMzAt(q - 1) <= back.getMzAt(q),
                        "spectrum " + k + " came back unordered at " + q);
            }
        }
    }

    /** the same at a deliberately unaligned starting offset */
    @Test
    public void aSpectrumSurvivesAnUnalignedOffset() {
        final SpectrumDatatype type = new SpectrumDatatype();
        for (int pad = 0; pad < 9; ++pad) {
            final WriteBuffer out = new WriteBuffer();
            for (int p = 0; p < pad; ++p) out.put((byte) 0x5a);
            final SimpleSpectrum in = spectrumOf(116);
            type.write(out, in);
            final ByteBuffer bytes = out.getBuffer();
            bytes.flip();
            for (int p = 0; p < pad; ++p) bytes.get();
            final SimpleSpectrum back = type.read(bytes);
            assertEquals(in.size(), back.size(), "size at pad " + pad);
            for (int q = 0; q < in.size(); ++q) {
                assertEquals(in.getMzAt(q), back.getMzAt(q), 0d, "mass " + q + " at pad " + pad);
            }
        }
    }

    @Test
    public void aSpectrumSurvivesTheRoundTripUnchanged() {
        for (int size : new int[]{1, 2, 3, 7, 16, 17, 63, 64, 111, 116, 1000}) {
            final SimpleSpectrum in = spectrumOf(size);
            final SimpleSpectrum out = roundTrip(in);
            assertEquals(in.size(), out.size(), "size, at " + size);
            for (int k = 0; k < in.size(); ++k) {
                assertEquals(in.getMzAt(k), out.getMzAt(k), 0d, "mass " + k + " of " + size);
                // intensities are stored as float, so only that much precision is promised
                assertEquals(in.getIntensityAt(k), out.getIntensityAt(k), 1e-3 * Math.abs(in.getIntensityAt(k)) + 1e-6,
                        "intensity " + k + " of " + size);
            }
        }
    }

    /**
     * And it has to come back sorted, because it comes back declaring that it is. A spectrum that lies about being
     * ordered breaks every binary search over it, not only the one that noticed.
     */
    @Test
    public void aSpectrumComesBackOrdered() {
        for (int size : new int[]{2, 7, 17, 111, 116, 1000}) {
            final SimpleSpectrum out = roundTrip(spectrumOf(size));
            for (int k = 1; k < out.size(); ++k) {
                assertTrue(out.getMzAt(k - 1) <= out.getMzAt(k),
                        "not ordered at index " + k + " of " + size + ": "
                                + out.getMzAt(k - 1) + " > " + out.getMzAt(k));
            }
            // and the binary search must agree with a linear scan for every peak it contains
            for (int k = 0; k < out.size(); ++k) {
                final int found = Spectrums.mostIntensivePeakWithin(out, out.getMzAt(k),
                        new de.unijena.bioinf.ChemistryBase.ms.Deviation(1, 1e-6));
                assertTrue(found >= 0, "binary search lost the peak at index " + k + " of " + size);
            }
        }
    }
}
