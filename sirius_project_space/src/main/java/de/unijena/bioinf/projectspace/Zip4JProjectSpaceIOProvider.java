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

import net.lingala.zip4j.ZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class Zip4JProjectSpaceIOProvider implements ProjectIOProvider<Zip4JProjectSpaceIO, Zip4JProjectSpaceReader, Zip4JProjectSpaceWriter> {
    protected final ReadWriteLock rwLock =  new ReentrantReadWriteLock();
    protected final ZipFile zipLocation;

    public Zip4JProjectSpaceIOProvider(@NotNull Path location) {
        this(new ZipFile(location.toFile()));
    }

    public Zip4JProjectSpaceIOProvider(@NotNull ZipFile zipLocation) {
        this.zipLocation = zipLocation;
    }

    @Override
    public Zip4JProjectSpaceIO newIO(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new Zip4JProjectSpaceIO(zipLocation, rwLock, propertyGetter);
    }

    @Override
    public Zip4JProjectSpaceReader newReader(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new Zip4JProjectSpaceReader(zipLocation, rwLock, propertyGetter);
    }

    @Override
    public Zip4JProjectSpaceWriter newWriter(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new Zip4JProjectSpaceWriter(zipLocation, rwLock, propertyGetter);
    }

    @Override
    public Path getLocation() {
        return zipLocation.getFile().toPath();
    }

    @Override
    public void flush() {
       //no reopen needed writes instantly
    }

    @Override
    public void close() throws IOException {
        zipLocation.close();
    }
}
