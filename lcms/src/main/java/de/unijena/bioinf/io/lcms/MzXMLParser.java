package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MzXMLParser implements LCMSParser{

    @Override
    public LCMSRun parse(File file, SpectrumStorage storage) throws IOException {
        final LCMSRun run = new LCMSRun(new DataSource(file));
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(file,new MzXMLSaxParser(run, storage));
        } catch (SAXException|ParserConfigurationException e) {
            throw new IOException(e);
        }
        run.setReference(new MsDataSourceReference(file.getParentFile().toURI(), file.getName(), null, null));
        return run;
    }

    public LCMSRun parse(DataSource source, InputSource input, SpectrumStorage storage) throws IOException {
        final LCMSRun run = new LCMSRun(source);
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(input,new MzXMLSaxParser(run, storage));
        } catch (SAXException|ParserConfigurationException e) {
            throw new IOException(e);
        }
        {
            // get source location
            try {
                URI s = source.getUrl().toURI();
                URI parent = s.getPath().endsWith("/") ? s.resolve("..") : s.resolve(".");
                String fileName = parent.relativize(s).toString();
                run.setReference(new MsDataSourceReference(parent, fileName, null, null));
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        return run;
    }

}
