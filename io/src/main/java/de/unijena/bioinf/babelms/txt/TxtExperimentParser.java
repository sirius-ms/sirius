package de.unijena.bioinf.babelms.txt;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.massbank.MassbankExperimentParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class TxtExperimentParser implements Parser<Ms2Experiment> {

    MassbankExperimentParser delegate = new MassbankExperimentParser();

    InputStream lastSeenInputStream = null;
    BufferedReader lastWrappingReader = null;

    @Override
    public Ms2Experiment parse(BufferedReader reader, URI source) throws IOException {
        try {
            return delegate.parse(reader, source);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse MassBank .txt file", e);
        }
    }

    @Override
    public Ms2Experiment parse(InputStream inputStream, URI source) throws IOException {
        if (inputStream != lastSeenInputStream) {
            lastSeenInputStream = inputStream;
            lastWrappingReader = FileUtils.ensureBuffering(new InputStreamReader(inputStream));
        }
        return parse(lastWrappingReader, source);
    }
}
