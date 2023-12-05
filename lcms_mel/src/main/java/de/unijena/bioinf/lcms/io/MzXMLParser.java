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

package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.ms.persistence.model.core.ChromatographyType;
import de.unijena.bioinf.ms.persistence.model.core.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.Run;
import de.unijena.bioinf.ms.persistence.model.core.Scan;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class MzXMLParser implements LCMSParser {

    @Override
    public void parse(
            File file,
            IOThrowingConsumer<Run> runConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            Run.RunBuilder defaultRun
    ) throws IOException {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(file,
                    new MzXMLSaxParser(file.getName(), runConsumer, scanConsumer, msmsScanConsumer, defaultRun, this));
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/home/mel/lcms-data/220331AliW_Mut_LytM.mzXML");
        AtomicInteger scans = new AtomicInteger(0);
        AtomicInteger ms2scans = new AtomicInteger(0);
        new MzXMLParser().parse(f, System.out::println, ms -> scans.addAndGet(1), msms -> ms2scans.addAndGet(1), Run.builder().runType(Run.Type.SAMPLE).chromatography(ChromatographyType.LC));
        System.out.println(scans);
        System.out.println(ms2scans);
    }

}
