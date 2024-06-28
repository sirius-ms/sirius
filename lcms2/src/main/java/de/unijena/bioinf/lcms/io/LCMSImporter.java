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
import de.unijena.bioinf.lcms.projectspace.SiriusDatabaseAdapter;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class LCMSImporter {

    public static ProcessedSample importToProject(
            Path file,
            LCMSStorageFactory storageFactory,
            SiriusDatabaseAdapter siriusDatabaseAdapter,
            boolean saveRawScans,
            Chromatography chromatography
    ) throws IOException {
        LCMSParser parser;
        if (file.toString().toLowerCase().endsWith(".mzml")) {
            parser = new MzMLParser();
        } else if (file.toString().toLowerCase().endsWith(".mzxml")) {
            parser = new MzXMLParser();
        } else {
            throw new IOException("Illegal file extension. Only .mzml and .mzxml are supported");
        }
        LCMSRun run = LCMSRun.builder().chromatography(chromatography).build();
        if (!saveRawScans) {
            return parser.parse(file, storageFactory, siriusDatabaseAdapter::importRun, siriusDatabaseAdapter::updateRun, null, null, run);
        } else {
            return parser.parse(file, storageFactory, siriusDatabaseAdapter::importRun, siriusDatabaseAdapter::updateRun, siriusDatabaseAdapter::importScan, siriusDatabaseAdapter::importMSMSScan, run);
        }
    }

    public static ProcessedSample importToProject(
            URI source,
            LCMSStorageFactory storageFactory,
            SiriusDatabaseAdapter siriusDatabaseAdapter,
            boolean saveRawScans,
            Chromatography chromatography
    ) throws IOException {
        LCMSParser parser;
        if (source.getPath().toLowerCase().endsWith(".mzml")) {
            parser = new MzMLParser();
        } else if (source.getPath().toLowerCase().endsWith(".mzxml")) {
            parser = new MzXMLParser();
        } else {
            throw new IOException("Illegal file extension. Only .mzml and .mzxml are supported");
        }
        LCMSRun run = LCMSRun.builder().chromatography(chromatography).build();
        if (!saveRawScans) {
            return parser.parse(source, storageFactory, siriusDatabaseAdapter::importRun, siriusDatabaseAdapter::updateRun, null, null, run);
        } else {
            return parser.parse(source, storageFactory, siriusDatabaseAdapter::importRun, siriusDatabaseAdapter::updateRun, siriusDatabaseAdapter::importScan, siriusDatabaseAdapter::importMSMSScan, run);
        }
    }

}
