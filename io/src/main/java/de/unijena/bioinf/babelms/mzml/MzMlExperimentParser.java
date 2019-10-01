package de.unijena.bioinf.babelms.mzml;

import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.model.lcms.LCMSRun;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

public class MzMlExperimentParser extends AbstractMzParser {

    protected URL currentSource;

    @Override
    protected boolean setNewSource(BufferedReader sourceReader, URL sourceURL) {
        if (!currentSource.equals(sourceURL)) {
            currentSource = sourceURL;
            return true;
        }
        return false;
    }

    @Override
    protected LCMSRun parseToLCMSRun(BufferedReader sourceReader, URL sourceURL) throws IOException {
        return new MzMLParser().parse(currentSource, inMemoryStorage);
    }
}
