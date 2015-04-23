package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.io.*;
import java.util.Iterator;

public abstract class SpectralParser {

    public Iterator<Ms2Spectrum<Peak>> parseSpectra(File file) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        return parseSpectra(reader);
    }

    public Iterator<Ms2Spectrum<Peak>> parseSpectra(InputStream instream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
        return parseSpectra(reader);
    }

    public Iterator<Ms2Spectrum<Peak>> parseSpectra(Reader reader) throws IOException {
        if (!(reader instanceof BufferedReader)) reader = new BufferedReader(reader);
        return parseSpectra((BufferedReader)reader);
    }

    public abstract Iterator<Ms2Spectrum<Peak>> parseSpectra(BufferedReader reader) throws IOException;

}
