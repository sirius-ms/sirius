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
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ZipFSProjectSpaceIOProvider extends FileProjectSpaceIOProvider {

    public ZipFSProjectSpaceIOProvider(@NotNull Path location, boolean createNew, boolean useTempFile) {
        this(location, createNew, useTempFile, PropertyManager.getInteger("de.unijena.bioinf.sirius.zipfs.maxWritesBeforeFlush", null, 250));
    }

    public ZipFSProjectSpaceIOProvider(@NotNull Path location, boolean createNew, boolean useTempFile, int maxWritesBeforeFlush) {
        super(() -> {
            try {
                if (Files.exists(location) && !Files.isRegularFile(location))
                    throw new IllegalArgumentException("Compressed Location must be a regular file!");
                return new ZipFSWrapper(location, createNew, useTempFile, maxWritesBeforeFlush);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    ZipFSWrapper getFS() {
        return (ZipFSWrapper) fs;
    }

    static class ZipFSWrapper implements FSWrapper {
        private final Lock lock = new ReentrantLock();
        private final Path location; // the location on the default (real) fs
        private final boolean useTempFile;
        private final int maxWrites;
        private int writes = 0;

        private FileSystem zipFS;

        private ZipFSWrapper(Path location, boolean createNew, boolean useTempFile) throws IOException {
            this(location, createNew, useTempFile, 0);
        }

        public ZipFSWrapper(Path location, boolean createNew, boolean useTempFile, int maxWrites) throws IOException {
            this.location = location;
            this.useTempFile = useTempFile;
            this.maxWrites = maxWrites;
            this.zipFS = FileUtils.asZipFS(location, createNew, useTempFile);
        }

        private Path resolvePath(String relative) {
            if (relative == null || relative.isBlank() || relative.equals("/") || relative.equals(zipFS.getSeparator()))
                return zipFS.getPath(zipFS.getSeparator());
            return zipFS.getPath(relative);
        }

        @Override
        public void writeFS(String relativeFrom, String relativeTo, IOFunctions.BiIOConsumer<Path, Path> writeWithFS) throws IOException {
            lock.lock();
            try {
                ensureOpen();
                Path pFrom = resolvePath(relativeFrom);
                Path pTo = resolvePath(relativeTo);
                writeWithFS.accept(pFrom, pTo);
                ensureWrite();
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                flushAndReloadZipFS();
                throw e;
            } finally {
                lock.unlock();
            }
        }

        public void writeFS(String relative, IOFunctions.IOConsumer<Path> writeWithFS) throws IOException {
            lock.lock();
            try {
                ensureOpen();
                Path p = resolvePath(relative);
                writeWithFS.accept(p);
                ensureWrite();
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                flushAndReloadZipFS();
                throw e;
            } finally {
                lock.unlock();
            }
        }

        public void readFS(String relative, IOFunctions.IOConsumer<Path> readWithFS) throws IOException {
            readFS(relative, p -> {
                readWithFS.accept(p);
                return null;
            });
        }

        public <R> R readFS(String relative, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            lock.lock();
            try {
                ensureOpen();
                Path p = resolvePath(relative);
                return readWithFS.apply(p);
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                flushAndReloadZipFS();
                throw e;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Path getLocation() {
            return location;
        }

        private void ensureOpen() throws IOException {
            lock.lock();
            try {
                if (!zipFS.isOpen()) {
                    LoggerFactory.getLogger(getClass()).warn("ZipFS seems to be interrupted!");
                    flushAndReloadZipFS();
                }
            } finally {
                lock.unlock();
            }
        }

        private void ensureWrite() throws IOException {
            if (maxWrites <= 0) // disabled
                return;
            lock.lock();
            try {
                if (++writes >= maxWrites) {
                    flushAndReloadZipFS();
                }
            } finally {
                lock.unlock();
            }
        }

        private void flushAndReloadZipFS() throws IOException {
            lock.lock();
            try {
                try {
                    if (zipFS.isOpen())
                        zipFS.close();
    //            } catch (ClosedChannelException e) {
    //                LoggerFactory.getLogger(getClass()).error("Could not close ZipFS due to ClosedChannelException usually caused by a thread level interrupt. Try to delete lock and reopen!", e);
    //                ((ZipFileSystemProvider)zipFS.provider()).removeFileSystem(location, (ZipFileSystem) zipFS); //todo find out how to access  api via gradle
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Could not close ZipFS. Try to ignore and reopen!", e);
                    throw e;
                }
                zipFS = FileUtils.asZipFS(location, false, useTempFile);
                writes = 0;
            } finally {
                lock.unlock();
            }
        }

        public void close() throws IOException {
            if (zipFS.isOpen())
                zipFS.close();
        }
    }
}