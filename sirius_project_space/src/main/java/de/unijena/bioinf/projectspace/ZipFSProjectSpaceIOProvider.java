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

import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
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
import java.util.concurrent.atomic.AtomicInteger;

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
        private final ReentrantReadWriteUpdateLock lock;
        private final ZipFSTreeNode root;
        private CompressionFormat format;

        public ZipFSTree(Path location, boolean useTempFile, int maxWrites, CompressionFormat format) throws IOException {
            this.format = format;
            lock = new ReentrantReadWriteUpdateLock();
            root = new ZipFSTreeNode(null, location, useTempFile, format.getRootCompression(), maxWrites, lock);
        }

        private ResolvedPath resolvePath(String relative, boolean isDir) throws IOException {
            ZipFSTreeNode zipFSNode = root;
            lock.updateLock().lock();
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

                        {
                            ZipFSTreeNode zipFSNodeTmp = zipFSNode.childFileSystems.get(prefix);
                            if (zipFSNodeTmp == null || !zipFSNodeTmp.zipFS.isOpen()) {
                                lock.writeLock().lock();
                                try {
                                    if (!zipFSNode.childFileSystems.containsKey(prefix)) {
                                        zipFSNode.childFileSystems.put(prefix, new ZipFSTreeNode(
                                                zipFSNode, prefix, zipFSNode.useTempFile, format.getCompression(idx), zipFSNode.maxWrites, lock));
                                    } else {
                                        zipFSNode.childFileSystems.get(prefix).ensureOpen();
                                    }
                                    zipFSNode = zipFSNode.childFileSystems.get(prefix);
                                } finally {
                                    lock.writeLock().unlock();
                                }
                            } else {
                                zipFSNode = zipFSNodeTmp;
                            }
                        }
                    }
                    return new ResolvedPath(zipFSNode, zipFSNode.resolveCurrentPath((source != prefix ? prefix.relativize(source) : source).toString()));
                } else {
                    return new ResolvedPath(zipFSNode, zipFSNode.resolveCurrentPath(null)); //root
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                zipFSNode.flushAndReloadZipFS();
                throw e;
            } finally {
                lock.updateLock().unlock();
            }
        }

        @Override
        public void writeFile(String relativeFrom, String relativeTo, IOFunctions.BiIOConsumer<Path, Path> writeWithFS) throws IOException {
            ResolvedPath fsPFrom = null;
            ResolvedPath fsPTo = null;
            try {
                lock.updateLock().lock();
                try {
                    fsPFrom = resolvePath(relativeFrom, false); //copy zipped sub fs as file without decompressing it
                    fsPTo = resolvePath(relativeTo, false); //copy zipped sub fs as file without decompressing it
                } finally {
                    lock.updateLock().unlock();
                }

                lock.readLock().lock();
                try {
                    writeWithFS.accept(fsPFrom.path, fsPTo.path);
                } finally {
                    lock.readLock().unlock();
                }
                fsPTo.fs.ensureWrite();
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                if (fsPFrom != null)
                    fsPFrom.fs.flushAndReloadZipFS();
                if (fsPTo != null)
                    fsPTo.fs.flushAndReloadZipFS();
                throw e;
            }
        }

        public void writeFile(String relative, IOFunctions.IOConsumer<Path> writeWithFS) throws IOException {
            ResolvedPath fs = null;
            try {
                fs = resolvePath(relative, false);
                lock.readLock().lock();
                try {
                    writeWithFS.accept(fs.path);
                } finally {
                    lock.readLock().unlock();
                }
                fs.fs.ensureWrite();
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                if (fs != null)
                    fs.fs.flushAndReloadZipFS();
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
            ResolvedPath fs = null;
            try {
                fs = resolvePath(relative, isDir);
                lock.readLock().lock();
                try {
                    return readWithFS.apply(fs.path);
                } finally {
                    lock.readLock().unlock();
                }
            } catch (ClosedByInterruptException e) {
                LoggerFactory.getLogger(getClass()).warn("FS operation cancelled by interruption. Connection to ZipFS Lost. Try reopening!");
                if (fs != null)
                    fs.fs.flushAndReloadZipFS();
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
        private final ReentrantReadWriteUpdateLock lock;
        private final Path location; // the location on the default (real) fs
        private final boolean useTempFile;
        private final ZipCompressionMethod compressionMethod;
        //todo check if root only flush improves performance
        private final int maxWrites;
        private final AtomicInteger writes = new AtomicInteger(0);

        @Nullable
        private final ZipFSTreeNode parent;

        private FileSystem zipFS;

        private final Map<Path, ZipFSTreeNode> childFileSystems = new HashMap<>();

        private ZipFSTreeNode(@Nullable ZipFSTreeNode parent, Path location, boolean useTempFile, ZipCompressionMethod compressionMethod, int maxWrites, ReentrantReadWriteUpdateLock lock) throws IOException {
            this(lock, parent, location, Files.notExists(location), useTempFile, compressionMethod, maxWrites);
        }

        private ZipFSTreeNode(ReentrantReadWriteUpdateLock lock, @Nullable ZipFSTreeNode parent, Path location, boolean createNew, boolean useTempFile, ZipCompressionMethod compressionMethod, int maxWrites) throws IOException {
            this.lock = lock;
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
                return zipFS.getPath(zipFS.getSeparator());
            else
                return zipFS.getPath(relative);
        }

        private boolean isRoot() {
            return parent == null;
        }

        private void ensureOpen() throws IOException {
            if (!zipFS.isOpen()) {
                try {
                    lock.writeLock().lock();
                    if (!zipFS.isOpen()) {
                        LoggerFactory.getLogger(getClass()).warn("ZipFS seems to be interrupted!");
                        flushAndReloadZipFS();
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        private void ensureWrite() throws IOException {
            if (maxWrites <= 0) // disabled
                return;
            if (writes.incrementAndGet() >= maxWrites) {
                lock.writeLock().lock();
                try {
                    if (writes.get() >= maxWrites)
                        flushAndReloadZipFS();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        // RUN ONLY WITHIN LOCKED BLOCK
        private void flushAndReloadZipFS() throws IOException {
            lock.writeLock().lock();
            try {
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
                writes.set(0);
            } finally {
                lock.writeLock().unlock();
            }
        }

        // RUN ONLY WITHIN LOCKED BLOCK
        public void close() throws IOException {
            lock.writeLock().lock();
            try {
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
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public int compareTo(@NotNull ZipFSTreeNode o) {
            return location.compareTo(o.location);
        }
    }

    private static class ResolvedPath {
        private final ZipFSTreeNode fs;
        private final Path path;

        private ResolvedPath(ZipFSTreeNode fs, Path path) {
            this.fs = fs;
            this.path = path;
        }
    }
}