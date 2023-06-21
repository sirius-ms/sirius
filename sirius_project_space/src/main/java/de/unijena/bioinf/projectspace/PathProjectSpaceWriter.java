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
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class PathProjectSpaceWriter extends PathProjectSpaceIO implements ProjectWriter {

    public PathProjectSpaceWriter(FileSystemManager fs, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(fs, propertyGetter);
    }

    @Override
    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func) throws IOException {
        fs.writeFile(resolve(relativePath), p -> {
            try (final BufferedWriter stream = Files.newBufferedWriter(mkFilePath(p))) {
                func.accept(stream);
            }
        });

    }

    @Override
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<OutputStream> func) throws IOException {
        fs.writeFile(resolve(relativePath), p -> {
            try (final OutputStream stream = Files.newOutputStream(mkFilePath(p))) {
                func.accept(stream);
            }
        });
    }

    @Override
    public void keyValues(String relativePath, Map<?, ?> map) throws IOException {
        fs.writeFile(resolve(relativePath), p -> {
            try (final BufferedWriter stream = Files.newBufferedWriter(mkFilePath(p))) {
                FileUtils.writeKeyValues(stream, map);
            }
        });
    }

    @Override
    public void table(String relativePath, @Nullable  String[] header, Iterable<String[]> rows) throws IOException {
        fs.writeFile(resolve(relativePath), p -> {
            try (final BufferedWriter bw = Files.newBufferedWriter(mkFilePath(p))) {
                FileUtils.writeTable(bw, header, rows);
            }
        });
    }

    @Override
    public void delete(String relativePath) throws IOException {
        fs.delete(resolve(relativePath), true);
    }

    @Override
    public void deleteIfExists(String relativePath) throws IOException {
        fs.delete(resolve(relativePath), false);
    }

    @Override
    public void move(String directoryName, String newDirName) throws IOException {
        fs.writeFile(directoryName, newDirName, Files::move);
    }

    protected static Path mkFilePath(Path file) throws IOException {
        if (file.getParent() != null)
            Files.createDirectories(file.getParent());
        return file;
    }
}