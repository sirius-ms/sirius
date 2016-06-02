/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
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
