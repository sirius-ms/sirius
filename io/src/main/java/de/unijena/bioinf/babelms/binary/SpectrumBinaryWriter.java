package de.unijena.bioinf.babelms.binary;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by kaidu on 20.06.2015.
 */
public class SpectrumBinaryWriter {

    public static void writeSpectra(OutputStream out, SimpleSpectrum[] spectra) throws IOException {
        final DataOutputStream outputStream = new DataOutputStream(out);
        writeSpectra(outputStream, spectra);
    }

    public static void writeSpectra(DataOutputStream out, SimpleSpectrum[] spectra) throws IOException {
        out.writeInt(spectra.length);
        for (int i=0; i < spectra.length; ++i) {
            final SimpleSpectrum spec = spectra[i];
            out.writeInt(spec.size());
            for (int j=0; j < spec.size(); ++j) {
                out.writeDouble(spec.getMzAt(j));
            }
            for (int j=0; j < spec.size(); ++j) {
                out.writeDouble(spec.getIntensityAt(j));
            }
        }
    }

}
