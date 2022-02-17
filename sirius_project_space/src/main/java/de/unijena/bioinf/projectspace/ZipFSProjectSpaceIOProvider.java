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
import de.unijena.bioinf.ChemistryBase.utils.ZipCompressionMethod;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ZipFSProjectSpaceIOProvider extends FileProjectSpaceIOProvider {

    public ZipFSProjectSpaceIOProvider(@NotNull Path location, boolean useTempFile) {
        this(location, useTempFile,
                PropertyManager.getInteger("de.unijena.bioinf.sirius.zipfs.maxWritesBeforeFlush", 250),
                CompressionFormat.of(
                        PropertyManager.getProperty("de.unijena.bioinf.sirius.zipfs.compressionLevels"),
                        PropertyManager.getProperty("de.unijena.bioinf.sirius.zipfs.compression")
                )
        );
    }

    public ZipFSProjectSpaceIOProvider(@NotNull Path location, boolean useTempFile, int maxWritesBeforeFlush, CompressionFormat compFormat) {
        super(() -> {
            try {
                if (Files.exists(location) && !Files.isRegularFile(location))
                    throw new IllegalArgumentException("Compressed Location must be a regular file!");
                return new ZipFSTree(location, useTempFile, maxWritesBeforeFlush, compFormat);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void flush() throws IOException {
        getFSManager().flush();
    }

    @Override
    public CompressionFormat getCompressionFormat() {
        return super.getCompressionFormat();
    }

    @Override
    public void setCompressionFormat(CompressionFormat format) {
        getFSManager().format = format;
    }

    ZipFSTree getFSManager() {
        return (ZipFSTree) fsManager;
    }

    static class ZipFSTree implements FileSystemManager {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final ZipFSTreeNode root;
//        private final int[] sortedSubFSLevels;
        private CompressionFormat format;

        public ZipFSTree(Path location, boolean useTempFile, int maxWrites, CompressionFormat format) throws IOException {
            this.format = format;
            root = new ZipFSTreeNode(null, location, useTempFile, format.getRootCompression(), maxWrites);
        }

        private ZipFSTreeNode resolvePath(String relative, boolean isDir) throws IOException {
            ZipFSTreeNode zipFSNode = root;
            try {
                if (relative != null) {
                    final Path source = zipFSNode.zipFS.getPath(relative);
                    Path prefix = source;
                    for (int idx : format.compressionLevels) {
                        if (idx >= source.getNameCount())
                            break;
                        if (idx == source.getNameCount() - 1 && !isDir) //path is a file a cannot be a sub fs
                            break;
                        prefix = source.subpath(0, idx + 1);

                        if (!zipFSNode.childFileSystems.containsKey(prefix)) {
                            zipFSNode.childFileSystems.put(prefix, new ZipFSTreeNode(
                                    zipFSNode, prefix, zipFSNode.useTempFile, format.getCompression(idx), zipFSNode.maxWrites));
                        }
                        zipFSNode = zipFSNode.childFileSystems.get(prefix);
                        zipFSNode.ensureOpen();
                    }
                    zipFSNode.resolveCurrentPath((source != prefix ? prefix.relativize(source) : source).toString());
                } else {
                    zipFSNode.resolveCurrentPath(null); //root
                }
                return zipFSNode;
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                zipFSNode.flushAndReloadZipFS();
                throw e;
            }
        }

        @Override
        public void writeFile(String relativeFrom, String relativeTo, IOFunctions.BiIOConsumer<Path, Path> writeWithFS) throws IOException {
            ZipFSTreeNode fsPFrom = null;
            ZipFSTreeNode fsPTo = null;
            try {
                lock.writeLock().lock();
                try {
                    fsPFrom = resolvePath(relativeFrom, false); //copy zipped sub fs as file without decompressing it
                    fsPTo = resolvePath(relativeTo, false); //copy zipped sub fs as file without decompressing it
                    lock.readLock().lock();
                } finally {
                    lock.writeLock().unlock();
                }
                try {
                    writeWithFS.accept(fsPFrom.currentPath, fsPTo.currentPath);
                } finally {
                    lock.readLock().unlock();
                }
                lock.writeLock().lock();
                try {
                    fsPTo.ensureWrite();
                } finally {
                    lock.writeLock().unlock();
                }
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                if (fsPFrom != null)
                    fsPFrom.flushAndReloadZipFS();
                if (fsPTo != null)
                    fsPTo.flushAndReloadZipFS();
                throw e;
            }
        }

        public void writeFile(String relative, IOFunctions.IOConsumer<Path> writeWithFS) throws IOException {
            ZipFSTreeNode fs = null;
            try {
                lock.writeLock().lock();
                try {
                    fs = resolvePath(relative, false);
                   lock.readLock().lock();
                } finally {
                    lock.writeLock().unlock();
                }
                try {
                    writeWithFS.accept(fs.currentPath);
                } finally {
                    lock.readLock().unlock();
                }
                lock.writeLock().lock();
                try {
                    fs.ensureWrite();
                } finally {
                    lock.writeLock().unlock();
                }

            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                if (fs != null)
                    fs.flushAndReloadZipFS();
                throw e;
            }
        }

        public void readFile(String relative, IOFunctions.IOConsumer<Path> readWithFS) throws IOException {
            readFile(relative, p -> {
                readWithFS.accept(p);
                return null;
            });
        }

        public <R> R readFile(String relative, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            return readFile(relative, false, readWithFS);
        }

        @Override
        public <R> R withDir(@Nullable String relative, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            return readFile(relative, true, readWithFS);
        }

        private <R> R readFile(String relative, boolean isDir, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            ZipFSTreeNode fs = null;
            try {
                lock.writeLock().lock();
                try {
                    fs = resolvePath(relative, isDir);
                    lock.readLock().lock();
                } finally {
                    lock.writeLock().unlock();
                }
                try {
                    return readWithFS.apply(fs.currentPath);
                } finally {
                    lock.readLock().unlock();
                }
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                if (fs != null)
                    fs.flushAndReloadZipFS();
                throw e;
            }
        }


        @Override
        public Path getLocation() {
            return root.location;
        }

        public void close() throws IOException {
            lock.writeLock().lock();
            try {
                root.close();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void flush() throws IOException {
            lock.writeLock().lock();
            try {
                root.flushAndReloadZipFS();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private static class ZipFSTreeNode implements Closeable, Comparable<ZipFSTreeNode> {
        private final Path location; // the location on the default (real) fs
        private final boolean useTempFile;
        private final ZipCompressionMethod compressionMethod;

        private final int maxWrites;
        private int writes = 0;

        private Path currentPath = null;

        @Nullable
        private final ZipFSTreeNode parent;

        private FileSystem zipFS;

        Map<Path, ZipFSTreeNode> childFileSystems = new HashMap<>();

        private ZipFSTreeNode(@Nullable ZipFSTreeNode parent, Path location, boolean useTempFile, ZipCompressionMethod compressionMethod, int maxWrites) throws IOException {
            this(parent, location, Files.notExists(location), useTempFile, compressionMethod, maxWrites);
        }

        private ZipFSTreeNode(@Nullable ZipFSTreeNode parent, Path location, boolean createNew, boolean useTempFile, ZipCompressionMethod compressionMethod, int maxWrites) throws IOException {
            this.location = location;
            this.useTempFile = useTempFile;
            this.compressionMethod = compressionMethod;
            this.maxWrites = maxWrites;
            this.parent = parent;
            this.zipFS = FileUtils.asZipFS(location, createNew, useTempFile, compressionMethod);
        }

        /**
         * @param relative resolves path and caches int as currentPath
         * @return Resolved path or arror if path is not part of this FS
         */

        private Path resolveCurrentPath(String relative) {
            if (relative == null || relative.isBlank() || relative.equals("/") || relative.equals(zipFS.getSeparator()))
                setCurrentPath(zipFS.getPath(zipFS.getSeparator()));
            else
                setCurrentPath(zipFS.getPath(relative));
            return currentPath;
        }

        private void setCurrentPath(Path relative) {
            assert relative.getFileSystem().equals(zipFS);
            currentPath = relative;
        }

        private boolean isRoot() {
            return parent == null;
        }

        private void ensureOpen() throws IOException {
            if (!zipFS.isOpen()) {
                LoggerFactory.getLogger(getClass()).warn("ZipFS seems to be interrupted!");
                flushAndReloadZipFS();
            }

        }

        private void ensureWrite() throws IOException {
            if (maxWrites <= 0) // disabled
                return;
            if (++writes >= maxWrites) {
                flushAndReloadZipFS();
            }
        }

        private void flushAndReloadZipFS() throws IOException {
            try {
                if (zipFS.isOpen())
                    close();
                //            } catch (ClosedChannelException e) {
                //                LoggerFactory.getLogger(getClass()).error("Could not close ZipFS due to ClosedChannelException usually caused by a thread level interrupt. Try to delete lock and reopen!", e);
                //                ((ZipFileSystemProvider)zipFS.provider()).removeFileSystem(location, (ZipFileSystem) zipFS); //todo find out how to access  api via gradle
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Could not close ZipFS. Try to ignore and reopen!", e);
                throw e;
            }
            zipFS = FileUtils.asZipFS(location, false, useTempFile, compressionMethod);
            writes = 0;
        }

        public void close() throws IOException {

            ArrayList<ZipFSTreeNode> valuseTMP = new ArrayList<>(childFileSystems.values());
            for (ZipFSTreeNode childFS : valuseTMP) {
                try {
                    childFS.close();
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when closing child ZipFS: '" + childFS.location.toString(), e);
                }
            }

            if (zipFS.isOpen())
                zipFS.close();

            if (parent != null) {
                parent.childFileSystems.remove(this.location);
            }
        }

        @Override
        public int compareTo(@NotNull ZipFSTreeNode o) {
            return location.compareTo(o.location);
        }
    }
}