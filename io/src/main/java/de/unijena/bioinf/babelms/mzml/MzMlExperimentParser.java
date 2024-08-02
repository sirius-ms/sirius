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

package de.unijena.bioinf.babelms.mzml;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.model.lcms.LCMSRun;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class MzMlExperimentParser extends AbstractMzParser {

    public URI sourceId;
    protected File sourceFile;

    protected boolean setNewSource(Object sourceReaderOrStream, URI source) throws IOException {
        if (sourceId == null) {
            if (sourceReaderOrStream == null) {
                return false;
            } else {
                sourceId = source;
                sourceFile = createTempFile(sourceReaderOrStream, source);
                return true;
            }
        } else if (!source.equals(sourceId)) {
            sourceId = source;
            sourceFile = createTempFile(sourceReaderOrStream, source);
            return true;
        }
        return false;
    }

    private File createTempFile(Object sourceReaderOrStream, URI source) throws IOException {
        if (source != null && source.getScheme() != null && source.getScheme().equalsIgnoreCase("file")) {
            if (sourceReaderOrStream != null && sourceReaderOrStream instanceof Closeable c)
                c.close();
            return new File(source);
        } else {
            Path tmp = FileUtils.newTempFile("mzml_", ".mzml");
            try {
                try (BufferedWriter w = Files.newBufferedWriter(tmp)) {
                    if (sourceReaderOrStream instanceof InputStream ir)
                        IOUtils.copy(FileUtils.ensureBuffering(new InputStreamReader(ir)), w);
                    else if (sourceReaderOrStream instanceof Reader)
                        IOUtils.copy((Reader) sourceReaderOrStream, w);
                    else
                        throw new IllegalArgumentException("Only InputStream and Reader is supported!");
                }
            } finally {
                if (sourceReaderOrStream != null && sourceReaderOrStream instanceof Closeable c)
                    c.close();
            }
            File f = tmp.toFile();
            f.deleteOnExit();
            return f;
        }
    }


    @Override
    protected LCMSRun parseToLCMSRun() throws IOException {
        // we write buffer to local fs because if it is not it will be copied anyway
        return new MzMLParser().parse(sourceFile, inMemoryStorage);
    }
}
