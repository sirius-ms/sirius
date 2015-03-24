package de.unijena.bioinf.babelms.mzml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

public class MzmlParser {

    protected final XMLStreamReader reader;
    protected final static String NAMESPACE = "";

    public MzmlParser(InputStream instream) throws XMLStreamException {
        this.reader = XMLInputFactory.newInstance().createXMLStreamReader(instream);
    }

    protected void readHeader() throws XMLStreamException {
        // for the moment: ignore information in header
        findTag("msRun");

    }

    protected boolean findTag(String tagName) throws XMLStreamException {
        while (true) {
            final int eventId = reader.next();
            if (eventId == XMLStreamReader.START_ELEMENT) {
                final QName qname = reader.getName();
                if (isTag(tagName)) return true;
            } else if (eventId == XMLStreamReader.END_DOCUMENT) return false;
        }
    }

    protected boolean isTag(String tagName) {
        final QName qname = reader.getName();
        return qname.getLocalPart().equals(tagName) && qname.getNamespaceURI().equals("http://sashimi.sourceforge.net/schema_revision/mzXML_2.0");
    }

}
