/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import net.lingala.zip4j.ZipFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class Zip4JProjectSpaceReader extends Zip4JProjectSpaceIO implements ProjectReader {
    protected Zip4JProjectSpaceReader(ZipFile location, ReadWriteLock rwLock, Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        super(location, rwLock, propertyGetter);
    }

    @Override
    public <A> A textFile(String relativePath, IOFunctions.IOFunction<BufferedReader, A> func) throws IOException {
        return withReadLock(() -> {
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(zipLocation.getInputStream(resolveHeader(relativePath))))) {
                return func.apply(br);
            }
        });
    }

    @Override
    public <A> A binaryFile(String relativePath, IOFunctions.IOFunction<InputStream, A> func) throws IOException {
        return withReadLock(() -> {
            try (final InputStream stream = zipLocation.getInputStream(resolveHeader(relativePath))) {
                return func.apply(stream);
            }
        });
    }

    @Override
    public Map<String, String> keyValues(String relativePath) throws IOException {
        return withReadLock(() -> {
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(zipLocation.getInputStream(resolveHeader(relativePath))))) {
                return FileUtils.readKeyValues(br);
            }
        });
    }

    @Override
    public void table(String relativePath, boolean skipHeader, int fromLineInkl, int toLineExkl, Consumer<String[]> f) throws IOException {
        withReadLock(() -> {
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(zipLocation.getInputStream(resolveHeader(relativePath))))) {
                FileUtils.readTable(br, skipHeader, fromLineInkl, toLineExkl, f);
            }
        });
    }
}
