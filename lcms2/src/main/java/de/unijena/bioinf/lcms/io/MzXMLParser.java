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

import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.Run;
import de.unijena.bioinf.ms.persistence.model.core.Scan;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

public class MzXMLParser implements LCMSParser {

    @Override
    public ProcessedSample parse(
            File file,
            LCMSStorageFactory storageFactory,
            IOThrowingConsumer<Run> runConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            Run.RunBuilder runBuilder
    ) throws IOException {
        try {
            MzXMLSaxParser saxParser = new MzXMLSaxParser(
                    file.getName(),
                    storageFactory.createNewStorage(),
                    runConsumer, scanConsumer, msmsScanConsumer,
                    runBuilder,
                    this);
            SAXParserFactory.newInstance().newSAXParser().parse(file, saxParser);
            return saxParser.getProcessedSample();
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }
    }

//    public static void main(String[] args) throws IOException {
//        File f = new File("/home/mel/lcms-data/220331AliW_Mut_LytM.mzXML");
//        List<Scan> scans = new ArrayList<>();
//        List<MSMSScan> ms2scans = new ArrayList<>();
////        AtomicInteger scans = new AtomicInteger(0);
////        AtomicInteger ms2scans = new AtomicInteger(0);
//        ProcessedSample sample = new MzXMLParser().parse(
//                f,
//                LCMSStorage.temporaryStorage(),
//                System.out::println,
//                ms -> {
//                    ms.setScanId(new Random().nextLong());
//                    scans.add(ms);
//                },
//                msms -> {
//                    msms.setScanId(new Random().nextLong());
//                    ms2scans.add(msms);
//                },
////                ms -> scans.addAndGet(1),
////                msms -> ms2scans.addAndGet(1),
//                Run.builder().runType(Run.Type.SAMPLE).chromatography(ChromatographyType.LC)
//        );
//        System.out.println(sample.getRtSpan());
//        System.out.println(scans.size());
//        System.out.println(ms2scans.size());
//        System.out.println(sample.getTraceStorage().numberOfScans());
//
////        System.out.println(scans);
////        System.out.println(ms2scans);
//    }

}
