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
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.Run;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;

import java.io.File;
import java.io.IOException;

public class LCMSImporter {

    public static ProcessedSample importToProject(
            File source,
            LCMSStorageFactory storageFactory,
            MsProjectDocumentDatabase<? extends Database<?>> store,
            boolean saveRawScans,
            Run.Type runType,
            Chromatography chromatography
    ) throws IOException {
        LCMSParser parser;
        if (source.getName().toLowerCase().endsWith(".mzml")) {
            parser = new MzMLParser();
        } else if (source.getName().toLowerCase().endsWith(".mzxml")) {
            parser = new MzXMLParser();
        } else {
            throw new IOException("Illegal file extension. Only .mzml and .mzxml are supported");
        }
        Database<?> db = store.getStorage();
        Run.RunBuilder runBuilder = Run.builder().runType(runType).chromatography(chromatography);
        if (!saveRawScans) {
            return parser.parse(source, storageFactory, db::insert, db::upsert, null, null, runBuilder);
        } else {
            return parser.parse(source, storageFactory, db::insert, db::upsert, db::insert, db::insert, runBuilder);
        }
    }

}
