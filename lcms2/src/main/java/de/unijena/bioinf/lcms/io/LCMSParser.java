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
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public interface LCMSParser {

    @FunctionalInterface
    interface IOThrowingConsumer<T> {

        void consume(T object) throws IOException;

    }

    /**
     * Parse an LC/GC-MS file. The consumers need to handle to actual object storing and are required
     * to assign the object IDs.
     *
     * @param input Input location.
     * @param run  Initial {@link LCMSRun} isntance with default values for {@code Run.runType} and {@code Run.chromatography}.
     */
    default ProcessedSample parse(
            URI input,
            LCMSStorageFactory storageFactory,
            LCMSParser.IOThrowingConsumer<LCMSRun> runConsumer,
            LCMSParser.IOThrowingConsumer<LCMSRun> runUpdateConsumer,
            LCMSParser.IOThrowingConsumer<Scan> scanConsumer,
            LCMSParser.IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            LCMSRun run
    ) throws IOException{
        return parse(Path.of(input), storageFactory, runConsumer, runUpdateConsumer, scanConsumer, msmsScanConsumer, run);

    }

    ProcessedSample parse(
            Path input,
            LCMSStorageFactory storageFactory,
            LCMSParser.IOThrowingConsumer<LCMSRun> runConsumer,
            LCMSParser.IOThrowingConsumer<LCMSRun> runUpdateConsumer,
            LCMSParser.IOThrowingConsumer<Scan> scanConsumer,
            LCMSParser.IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            LCMSRun run
    ) throws IOException;
}