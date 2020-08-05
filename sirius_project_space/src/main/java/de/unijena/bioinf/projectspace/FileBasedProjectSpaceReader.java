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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class FileBasedProjectSpaceReader extends FileBasedProjectSpaceIO implements ProjectReader {

    FileBasedProjectSpaceReader(Path dir, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(dir,propertyGetter);
    }

    @Override
    public <A> A textFile(String relativePath, IOFunctions.IOFunction<BufferedReader, A> func)  throws IOException {
        try (final BufferedReader stream = Files.newBufferedReader(asPath(relativePath))) {
            return func.apply(stream);
        }
    }

    @Override
    public <A> A binaryFile(String relativePath, IOFunctions.IOFunction<BufferedInputStream, A> func)  throws IOException {
        try (final BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(asPath(relativePath)))) {
            return func.apply(stream);
        }
    }

    @Override
    public Map<String, String> keyValues(String relativePath) throws IOException {
        return FileUtils.readKeyValues(asPath(relativePath));
    }

    @Override
    public void table(String relativePath, boolean skipHeader, Consumer<String[]> f) throws IOException {
        try (final BufferedReader br = Files.newBufferedReader(asPath(relativePath))) {
            FileUtils.readTable(br, skipHeader, f);
        }
    }

}
