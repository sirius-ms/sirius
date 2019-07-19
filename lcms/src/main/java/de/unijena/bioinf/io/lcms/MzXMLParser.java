package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

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

    public LCMSRun parse(DataSource source, InputSource input, SpectrumStorage storage) throws IOException {
        final LCMSRun run = new LCMSRun(source);
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(input,new MzXMLSaxParser(run, storage));
        } catch (SAXException|ParserConfigurationException e) {
            throw new IOException(e);
        }
        return run;
    }

}
