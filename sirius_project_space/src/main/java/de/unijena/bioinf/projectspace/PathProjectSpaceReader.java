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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class PathProjectSpaceReader extends PathProjectSpaceIO implements ProjectReader {

    PathProjectSpaceReader(FileSystemManager fs, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(fs, propertyGetter);
    }

    @Override
    public <A> A textFile(String relativePath, IOFunctions.IOFunction<BufferedReader, A> func) throws IOException {
        return fs.readFile(resolve(relativePath), p -> {
            try (final BufferedReader stream = Files.newBufferedReader(p)) {
                return func.apply(stream);
            }
        });
    }

    @Override
    public <A> A binaryFile(String relativePath, IOFunctions.IOFunction<InputStream, A> func) throws IOException {
        return fs.readFile(resolve(relativePath), p -> {
            try (final InputStream stream = Files.newInputStream(p)) {
                return func.apply(stream);
            }
        });
    }

    @Override
    public Map<String, String> keyValues(String relativePath) throws IOException {
        return fs.readFile(resolve(relativePath), p -> {
            try (final BufferedReader br = Files.newBufferedReader(p)) {
                return FileUtils.readKeyValues(br);
            }
        });
    }

    @Override
    public void table(String relativePath, boolean skipHeader, int fromLineInkl, int toLineExkl, Consumer<String[]> f) throws IOException {
        fs.readFile(resolve(relativePath), p -> {
            try (final BufferedReader br = Files.newBufferedReader(p)) {
                FileUtils.readTable(br, skipHeader, fromLineInkl, toLineExkl, f);
            }
        });
    }
}