package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by kaidu on 20.06.2015.
 */
public class SpectrumBinaryReader {

    public static SimpleSpectrum[] readSpectra(DataInputStream in) throws IOException {
        final SimpleSpectrum[] spectra = new SimpleSpectrum[in.readInt()];
        for (int i=0; i < spectra.length; ++i) {
            final int N = in.readInt();
            final double[] mz = new double[N];
            final double[] ints = new double[N];
            for (int j=0; j < N; ++j) {
                mz[j] = in.readDouble();
            }
            for (int j=0; j < N; ++j) {
                ints[j] = in.readDouble();
            }
            spectra[i] = new SimpleSpectrum(mz, ints);
        }
        return spectra;
    }

}
