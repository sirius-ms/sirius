
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

package de.unijena.bioinf.babelms.pubchem;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.chemdb.CompoundQuery;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by kaidu on 24.03.14.
 */
public class Pubchem implements CompoundQuery {

    public static void main(String[] args) {
        final Pubchem pubchem = new Pubchem();
        final Set<MolecularFormula> formulas = pubchem.findMolecularFormulasByMass(
                PeriodicTable.getInstance().ionByNameOrThrow("[M+H]+").precursorMassToNeutralMass(314.1364), new Deviation(15, 0.0025));
        //formulas.addAll(new ChemSpider().findMolecularFormulasByMass(194.080376, new Deviation(5)));
        //System.out.println(formulas.size());
        //System.out.println(formulas);
    }

    @Override
    public Set<MolecularFormula> findMolecularFormulasByMass(double mass, double absoluteDeviation) {
        final String url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pccompound&retmax=100000&term="
            + enc(String.valueOf(mass - absoluteDeviation)) + ":" + enc(String.valueOf(mass + absoluteDeviation)) + "[exactmass]";
        final StringBuilder cids = new StringBuilder(1024);
        cids.append("cid=");
        final String sep = enc(",");
        sendGetRequest(url, new DefaultHandler() {
            private boolean listen = false;
            private StringBuilder buffer = new StringBuilder(64);

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes);
                listen = (qName.equals("Id"));
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);
                if (listen) {
                    cids.append(buffer.toString()).append(sep);
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
        if (cids.length() <= 4) return Collections.emptySet();
        cids.delete(cids.length() - sep.length(), cids.length());
        final String csv = sendPostRequestCSV("http://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/property/MolecularFormula/csv",
                cids.toString());
        final String[] rows = csv.split("\n");
        final String[] header = rows[0].split(",");
        int formulaIndex = -1;
        for (int i=0; i < header.length; ++i) if (header[i].equals("\"MolecularFormula\"")) {
            formulaIndex = i;
            break;
        }
        final HashSet<MolecularFormula> formulas = new HashSet<MolecularFormula>();
        if (formulaIndex<0) throw new RuntimeException("Unexpected result:\n" + csv );
        for (int k=1; k < rows.length; ++k) {
            final String[] row = rows[k].split(",");
            MolecularFormula.parseAndExecute(row[formulaIndex].substring(1, row[formulaIndex].length() - 1), formulas::add);
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

    @Override
    public Set<MolecularFormula> findMolecularFormulasByMass(double mass, Deviation allowedDeviation) {
        return findMolecularFormulasByMass(mass, allowedDeviation.absoluteFor(mass));
    }

    private static String sendPostRequestCSV(String uri, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(uri).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(body.length()));
            final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(body);
            writer.flush();
            final InputStream stream;
            try {
                stream = connection.getInputStream();
            } catch (IOException e) {
                final BufferedReader reader = FileUtils.ensureBuffering(new InputStreamReader(connection.getErrorStream()));
                final StringBuilder buffer = new StringBuilder(256);
                while (true) {
                    final String line = reader.readLine();
                    if (line==null) break;
                    buffer.append(line).append('\n');
                }
                System.err.println(buffer.toString());
                throw new RuntimeException(e);
            }
            final BufferedReader reader = FileUtils.ensureBuffering(new InputStreamReader(stream, "UTF-8"));
            final StringBuilder buffer = new StringBuilder(256);
            while (true) {
                final String line = reader.readLine();
                if (line==null) break;
                buffer.append(line).append('\n');
            }
            return buffer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection!=null) connection.disconnect();
        }
    }

    private static void sendGetRequest(String uri, DefaultHandler handler) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(uri).toURL().openConnection();
            connection.setRequestMethod("GET");
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(connection.getInputStream(), handler);
            connection.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
