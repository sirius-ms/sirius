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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class FileBasedProjectSpaceWriter extends FileBasedProjectSpaceIO implements ProjectWriter {

    public FileBasedProjectSpaceWriter(Path dir, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(dir, propertyGetter);
    }

    @Override
    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func) throws IOException {
        try (final BufferedWriter stream = Files.newBufferedWriter(resolveAndMkFilePath(relativePath))) {
            func.accept(stream);
        }
    }

    @Override
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<BufferedOutputStream> func) throws IOException {
        try (final BufferedOutputStream stream = new BufferedOutputStream(Files.newOutputStream(resolveAndMkFilePath(relativePath)))) {
            func.accept(stream);
        }
    }

    @Override
    public void keyValues(String relativePath, Map<?, ?> map) throws IOException {
        try (final BufferedWriter stream = Files.newBufferedWriter(resolveAndMkFilePath(relativePath))) {
            FileUtils.writeKeyValues(stream, map);
        }
    }

    @Override
    public void table(String relativePath, @Nullable  String[] header, Iterable<String[]> rows) throws IOException {
        try (final BufferedWriter bw = Files.newBufferedWriter(resolveAndMkFilePath(relativePath))) {
            FileUtils.writeTable(bw, header, rows);
        }
    }

    @Override
    public void delete(String relativePath) throws IOException {
        FileUtils.deleteRecursively((asPath(relativePath)));
    }

    @Override
    public void deleteIfExists(String relativePath) throws IOException {
        Files.deleteIfExists(asPath(relativePath));
    }

    protected Path resolveAndMkFilePath(String relativePath) throws IOException {
        Path file = asPath(relativePath);
        Files.createDirectories(file.getParent());
        return file;
    }
}
