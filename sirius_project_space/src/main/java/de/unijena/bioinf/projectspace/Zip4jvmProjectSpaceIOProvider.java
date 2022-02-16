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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class Zip4jvmProjectSpaceIOProvider implements ProjectIOProvider<Zip4jvmProjectSpaceIO, Zip4jvmProjectSpaceReader, Zip4jvmProjectSpaceWriter> {
    protected final ReadWriteLock rwLock =  new ReentrantReadWriteLock();
    protected final Path zipLocation;

    public Zip4jvmProjectSpaceIOProvider(@NotNull Path location) {
        this.zipLocation = location;
    }

    @Override
    public Zip4jvmProjectSpaceIO newIO(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new Zip4jvmProjectSpaceIO(zipLocation, rwLock, propertyGetter);
    }

    @Override
    public Zip4jvmProjectSpaceReader newReader(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new Zip4jvmProjectSpaceReader(zipLocation, rwLock, propertyGetter);
    }

    @Override
    public Zip4jvmProjectSpaceWriter newWriter(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new Zip4jvmProjectSpaceWriter(zipLocation, rwLock, propertyGetter);
    }

    @Override
    public Path getLocation() {
        return zipLocation;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws IOException {
        //nothing to do
    }
}
