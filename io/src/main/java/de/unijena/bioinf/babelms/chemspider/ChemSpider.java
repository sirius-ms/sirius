
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

package de.unijena.bioinf.babelms.chemspider;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.chemdb.CompoundQuery;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChemSpider implements CompoundQuery {


    @Override
    public Set<MolecularFormula> findMolecularFormulasByMass(double mass, Deviation allowedDeviation) {
        return findMolecularFormulasByMass(mass, allowedDeviation.absoluteFor(mass));
    }

    @Override
    public Set<MolecularFormula> findMolecularFormulasByMass(double mass, double range) {
        final String massStr = String.valueOf(mass);
        final String rangeStr = String.valueOf(range);
        final String url =  "http://www.chemspider.com/MassSpecAPI.asmx/SearchByMass2?mass=" + enc(massStr) + "&range=" +
                enc(rangeStr);
        final ArrayList<String> ids = new ArrayList<String>();
        sendGetRequest(url, new DefaultHandler(){
            private boolean listen = false;
            private StringBuilder buffer = new StringBuilder(64);

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);
                listen = (qName.equals("string"));
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);
                if (qName.equals("string") && listen) {
                    ids.add(buffer.toString().trim());
                    buffer.delete(0, buffer.length());
                    listen = false;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length);
                if (listen) buffer.append(ch, start, length);
            }
        });
        final HashSet<MolecularFormula> formulas = new HashSet<MolecularFormula>();
        for (String id : ids) {
            final String url2 = "http://www.chemspider.com/Search.asmx/GetRecordDetails?id=" + id;
            sendGetRequest(url2, new DefaultHandler(){
                final StringBuilder buffer = new StringBuilder(256);

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    super.endElement(uri, localName, qName);
                    if (buffer.length()>0) {
                        final String value = buffer.toString().trim();
                        if (value.startsWith("InChI=")) {
                            final String formula = value.split("/", 3)[1];
                            MolecularFormula.parseAndExecute(formula, formulas::add);
                        }
                        buffer.delete(0, buffer.length());
                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    super.characters(ch, start, length);
                    buffer.append(ch, start, length);
                }
            });
        }
        return formulas;
    }

    private static String enc(String xs) {
        try {
            return URLEncoder.encode(xs, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendGetRequest(String uri, DefaultHandler handler) {
        try {
            HttpURLConnection connection = (HttpURLConnection)URI.create(uri).toURL().openConnection();
            connection.setRequestMethod("GET");
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(connection.getInputStream(), handler);
            connection.disconnect();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LoggerFactory.getLogger(ChemSpider.class).error(e.getMessage(),e);
        }
    }

}
