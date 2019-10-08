package de.unijena.bioinf.babelms.mzml;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

public class MzXmlExperimentParser extends AbstractMzParser {

    protected BufferedReader currentSource;

    @Override
    protected boolean setNewSource(BufferedReader sourceReader, URL sourceURL) {
        if (!currentSource.equals(sourceReader)) {
            currentSource = sourceReader;
            return true;
        }
        return false;
    }

    @Override
    protected LCMSRun parseToLCMSRun(BufferedReader sourceReader, URL sourceURL) throws IOException {
        final MzXMLParser parser = new MzXMLParser();
        return parser.parse(new DataSource(sourceURL), new InputSource(currentSource), inMemoryStorage);
    }
}
