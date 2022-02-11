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

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class FileProjectSpaceIOProvider implements ProjectIOProvider<FileProjectSpaceIO, FileProjectSpaceReader, FileProjectSpaceWriter> {

    @NotNull
    protected final FSWrapper fs;

    public FileProjectSpaceIOProvider(@NotNull Path root) {
        this(() -> {
            if (Files.exists(root) && !Files.isDirectory(root))
                throw new IllegalArgumentException("Uncompressed Project-Space location must be a directory");
            return new FileFSWrapper(root);
        });
    }

    protected FileProjectSpaceIOProvider(@NotNull Supplier<FSWrapper> locationSupplier) {
        this.fs = locationSupplier.get();
    }


    @Override
    public FileProjectSpaceIO newIO(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new FileProjectSpaceIO(fs, propertyGetter);
    }

    @Override
    public FileProjectSpaceReader newReader(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new FileProjectSpaceReader(fs, propertyGetter);
    }

    @Override
    public FileProjectSpaceWriter newWriter(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new FileProjectSpaceWriter(fs, propertyGetter);
    }

    public Path getLocation() {
        return fs.getLocation();
    }

    public void close() throws IOException {
        fs.close();
    }

    static class FileFSWrapper implements FSWrapper {
        @NotNull
        private final Path root;

        FileFSWrapper(@NotNull Path root) {
            this.root = root;
        }

        @Override
        public void writeFS(String relativeFrom, String relativeTo, IOFunctions.BiIOConsumer<Path, Path> writeWithFS) throws IOException {
            writeWithFS.accept(resolvePath(relativeFrom), resolvePath(relativeTo));
        }

        @Override
        public void writeFS(String relative, IOFunctions.IOConsumer<Path> writeWithFS) throws IOException {
            writeWithFS.accept(resolvePath(relative));
        }

        @Override
        public void readFS(String relative, IOFunctions.IOConsumer<Path> readWithFS) throws IOException {
            readWithFS.accept(resolvePath(relative));

        }

        @Override
        public <R> R readFS(String relative, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            return readWithFS.apply(resolvePath(relative));
        }

        @Override
        public Path getLocation() {
            return root;
        }

        private Path resolvePath(String relative) {
            if (relative == null || relative.isBlank() || relative.equals("/"))
                return root;
            return root.resolve(relative);
        }

        @Override
        public void close() throws IOException {
            //nothing to do (default fs)
        }
    }
}