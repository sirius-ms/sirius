
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

public class PeriodicTableBlueObeliskReader extends PeriodicTableReader{


    @Override
    public void read(PeriodicTable table, Reader reader) throws IOException {
        read(table, new InputSource(reader));
    }

    @Override
    public void fromInputStream(PeriodicTable table, InputStream stream) throws IOException {
        read(table, new InputSource(stream));
    }

    public void readFromNet(PeriodicTable table) throws IOException{
        fromInputStream(table, new URL("http://bodr.svn.sourceforge.net/viewvc/bodr/trunk/bodr/elements/elements.xml").openStream());
    }

    public void readFromClasspath(PeriodicTable table) throws IOException{
        fromClassPath(table, "/blueobelisk_elements.xml");
    }

    public void read(PeriodicTable table, InputSource source) throws IOException {
        final Handler handler = new Handler(table);
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(source, handler);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    private static class Handler extends DefaultHandler {

        private final PeriodicTable table;

        private String symbol, name;
        private double mass;
        private int state, period, group;
        private char block;
        private final StringBuilder buffer;
        // state= 1->atom, 2->symbol, 3->name, 4->mass, 5->period, 6->group, 7->block

        private Handler(PeriodicTable table) {
            this.table = table;
            state=0;
            this.buffer = new StringBuilder();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (state == 0 && qName.equals("atom")) {
                state = 1;
                period=0;
                group=0;
                block='s';
                name="";
                symbol="";
                mass=0d;
            } else if (state == 1) {
                if (qName.equals("scalar")) {
                    final String type = attributes.getValue("dictRef");
                    if (type.equals("bo:exactMass")) {
                        state = 4;
                    } else if (type.equals("bo:period")) {
                        state = 5;
                    } else if (type.equals("bo:group")) {
                        state = 6;
                    } else if (type.equals("bo:periodTableBlock")) {
                        state = 7;
                    } else {
                        state=-1;
                    }
                } else if (qName.equals("label")) {
                    state = -1;
                    final String type = attributes.getValue("dictRef");
                    if (type.equals("bo:symbol")) {
                        symbol = attributes.getValue("value");
                    } else if (type.equals("bo:name") && "en".equals(attributes.getValue("xml:lang"))) {
                        name = attributes.getValue("value");
                    }
                } else {
                    state=-1;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (state) {
                case -1:
                    state = 1;
                    return;
                case 1:
                    table.addElement(name, symbol, mass, calculateValence());
                    state=0;
                    return;
                case 4:
                    mass = Double.parseDouble(buffer.toString());
                    break;
                case 5:
                    period = Integer.parseInt(buffer.toString());
                    break;
                case 6:
                    group = Integer.parseInt(buffer.toString());
                    break;
                case 7:
                    block = buffer.charAt(0);
                    break;
                default: return;
            }
            clearBuffer();
            state = 1;
        }

        private void clearBuffer() {
            buffer.delete(0, buffer.length());
        }

        private int calculateValence() {
            if (block == 'd') {
                // don't know the valence of metals =(
                return 2;
            } else {
                // calculate the valence as the minimal distance to the free or full valence orbital
                // cheat: don't parse the orbital but use the group and period information
                if (period > 1) {
                    return Math.min(8 - group, group);
                } else {
                    // valence orbital has only two slots
                    return (group==1 ? 1 : 0);
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (state>1) buffer.append(ch, start, length);
        }
    }
}
