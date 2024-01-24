/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.ChromatographyType;
import de.unijena.bioinf.ms.persistence.model.core.Run;
import de.unijena.bioinf.ms.persistence.model.core.SourceFile;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;

import java.io.File;
import java.io.IOException;

public class LCMSImporter {

    public static ProcessedSample importToProject(File source, MsProjectDocumentDatabase<? extends Database<?>> store) throws IOException {
        return importToProject(source, store, false, Run.Type.SAMPLE, ChromatographyType.LC);
    }

    public static ProcessedSample importToProject(
            File source,
            MsProjectDocumentDatabase<? extends Database<?>> store,
            boolean embedSource,
            Run.Type runType,
            ChromatographyType chromatographyType
    ) throws IOException {
        if (source.getName().toLowerCase().endsWith(".mzml")) {
            return importToProjectFromMZML(source, store, embedSource, runType, chromatographyType);
        } else if (source.getName().toLowerCase().endsWith(".mzxml")) {
            return importToProjectFromMZXML(source, store, embedSource, runType, chromatographyType);
        } else {
            throw new IOException("Illegal file extension. Only .mzml and .mzxml are supported");
        }
    }

    public static ProcessedSample importToProjectFromMZML(
            File source,
            MsProjectDocumentDatabase<? extends Database<?>> store,
            boolean embedSource,
            Run.Type runType,
            ChromatographyType chromatographyType
    ) throws IOException {
        return importToProject(source, SourceFile.Format.MZML, store, new MzMLParser(), embedSource, runType, chromatographyType);
    }

    public static ProcessedSample importToProjectFromMZXML(
            File source,
            MsProjectDocumentDatabase<? extends Database<?>> store,
            boolean embedSource,
            Run.Type runType,
            ChromatographyType chromatographyType
    ) throws IOException {
        return importToProject(source, SourceFile.Format.MZXML, store, new MzXMLParser(), embedSource, runType, chromatographyType);
    }

    private static ProcessedSample importToProject(
            File source,
            SourceFile.Format format,
            MsProjectDocumentDatabase<? extends Database<?>> store,
            LCMSParser parser,
            boolean embedSource,
            Run.Type runType,
            ChromatographyType chromatographyType
    ) throws IOException {
        Database<?> db = store.getStorage();
        Run.RunBuilder runBuilder = Run.builder().runType(runType).chromatography(chromatographyType);
        LCMSStorageFactory storageFactory = LCMSStorage.temporaryStorage();
        if (!embedSource) {
            return parser.parse(source, storageFactory, db::insert, db::insert, db::insert, runBuilder);
        } else {
            return parser.parse(source, storageFactory, format, db::insert, db::insert, db::insert, db::insert, runBuilder);
        }
    }

//    public static void main(String[] args) throws IOException {
//        File source = new File("/home/mel/lcms-data/polluted_citrus/G87532_1x_RD6_01_26277.mzML");
////        File source = new File("/home/mel/lcms-data/220331AliW_Mut_LytM.mzXML");
//        String storeLocation = "/home/mel/store.nitrite";
//        Files.delete(Path.of(storeLocation));
//        try (NitriteDatabase db = new NitriteDatabase(Path.of(storeLocation), SiriusProjectDocumentDatabase.buildMetadata())) {
//            SiriusProjectDatabaseImpl<? extends Database<?>> store = new SiriusProjectDatabaseImpl<>(db);
//            importToProject(source, store);
//
//            db.findAll(Run.class).forEach(System.out::println);
//            System.out.println(db.countAll(Scan.class));
//            System.out.println(db.countAll(MSMSScan.class));
//
//            Optional<MSMSScan> msms = db.findAllStr(MSMSScan.class).findFirst();
//            if (msms.isPresent()) {
//                Scan ms = db.findStr(Filter.build().eq("scanId", msms.get().getPrecursorScanId()), Scan.class).findFirst().orElseThrow();
//                System.out.println(msms.get().getScanId());
//                System.out.println(msms.get().getPrecursorScanId());
//                System.out.println(ms.getScanId());
//            }
//        }
//    }

}
