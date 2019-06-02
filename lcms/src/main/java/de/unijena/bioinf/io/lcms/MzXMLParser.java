package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.SpectrumStorage;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class MzXMLParser {

    public LCMSRun parse(File file, SpectrumStorage storage) throws IOException {
        final LCMSRun run = new LCMSRun(new DataSource(file));
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(file,new MzXMLSaxParser(run, storage));
        } catch (SAXException|ParserConfigurationException e) {
            throw new IOException(e);
        }
        return run;
    }

    // TODO: has to be buffered?
    public LCMSRun parse(InputStream inputStream, SpectrumStorage storage) throws IOException {
        final LCMSRun run = new LCMSRun(null);
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(inputStream,new MzXMLSaxParser(run, storage));
        } catch (SAXException|ParserConfigurationException e) {
            throw new IOException(e);
        }
        return run;
    }

}
