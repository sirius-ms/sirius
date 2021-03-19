
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
import gnu.trove.list.array.TDoubleArrayList;
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

public class IsotopicDistributionBlueObeliskReader extends DistributionReader {

    public IsotopicDistribution getFromClasspath() throws IOException {
        return super.fromClassPath("/blueobelisk_isotopes.xml");
    }

    public IsotopicDistribution getFromNet() throws IOException {
        return fromInputStream(new URL("http://bodr.svn.sourceforge.net/viewvc/bodr/trunk/bodr/isotopes/isotopes.xml").openStream());
    }

    public IsotopicDistribution read(InputSource source) throws IOException {
        try {
            final Handler handler = new Handler();
            SAXParserFactory.newInstance().newSAXParser().parse(source, handler);
            return handler.getDistribution();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public IsotopicDistribution read(Reader reader) throws IOException {
        return read(new InputSource(reader));
    }

    @Override
    public IsotopicDistribution fromInputStream(InputStream stream) throws IOException {
        return read(new InputSource(stream));
    }

    private static class Handler extends DefaultHandler {

        private final IsotopicDistribution dist;
        private String symbol;
        private final TDoubleArrayList masses, abundances;
        private double mass, abundance;
        private final StringBuilder buffer;
        private int state; // isoList=1, isotope=2, mass=3, abundance=4

        private Handler() {
            dist = new IsotopicDistribution(PeriodicTable.getInstance());
            this.masses = new TDoubleArrayList(7);
            this.abundances = new TDoubleArrayList(7);
            this.buffer = new StringBuilder();
        }

        public IsotopicDistribution getDistribution() {
            return dist;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (state==0 && qName.equals("isotopeList")) {
                state=1;
                symbol = attributes.getValue("id");
                masses.clear();
                abundances.clear();
            } if (state == 1 && qName.equals("isotope")) {
                state = 2;
                mass = 0;
                abundance = 0;
            } else if (state == 2 && qName.equals("scalar")) {
                final String type = attributes.getValue("dictRef");
                if("bo:relativeAbundance".equals(type)) {
                    state=4;
                } else if (("bo:exactMass").equals(type)) {
                    state = 3;
                } else {
                    state = -1;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (state == -1) {
                state = 2;
                return;
            }
            if (state == 1 && qName.equals("isotopeList")) {
                dist.addIsotope(symbol, masses.toArray(), abundances.toArray());
                state=0;
            } else if (state == 2 && qName.equals("isotope")) {
                if (abundance != 0) {
                    masses.add(mass);
                    abundances.add(abundance);
                }
                state = 1;
            } else if (state == 4) {
                state = 2;
                abundance = (Double.parseDouble(buffer.toString()) / 100d);
            } else if (state == 3) {
                state = 2;
                mass = (Double.parseDouble(buffer.toString()));
            } else return;
            buffer.delete(0, buffer.length());
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (state > 2) buffer.append(ch, start, length);
        }
    }
}
