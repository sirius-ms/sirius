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

import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZipFSProjectSpaceIOProvider extends PathProjectSpaceIOProvider {

    public ZipFSProjectSpaceIOProvider(@NotNull Path location, boolean useTempFile) {
        this(location, useTempFile,
                PropertyManager.getInteger("de.unijena.bioinf.sirius.zipfs.maxWritesBeforeFlush", 250),
                ZipProvider.getDefaultCompressionFormat()
        );
    }

    public ZipFSProjectSpaceIOProvider(@NotNull Path location, boolean useTempFile, int maxWritesBeforeFlush, CompressionFormat compFormat) {
        super(() -> {
            try {
                if (Files.exists(location) && !Files.isRegularFile(location))
                    throw new IllegalArgumentException("Compressed Location must be a regular file!");
                return new FSTree(location, useTempFile, 25, 125, compFormat, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /*static class ZipFSTree implements FileSystemManager {
        private final ReentrantReadWriteUpdateLock lock;
        private final ZipFSTreeNode root;
        private CompressionFormat format;

        private final int maxWrites;
        private boolean autoFlushEnabled = true;
        private final AtomicInteger writes = new AtomicInteger(0);

        public ZipFSTree(Path location, boolean useTempFile, int maxWrites, CompressionFormat format) throws IOException {
            this.maxWrites = maxWrites;
            setCompressionFormat(format);
            lock = new ReentrantReadWriteUpdateLock();
            root = new ZipFSTreeNode(null, location, useTempFile, format.getRootCompression(), lock);
        }

        public boolean isAutoFlushEnabled() {
            return autoFlushEnabled;
        }

        public void setAutoFlushEnabled(boolean autoFlushEnabled) {
            this.autoFlushEnabled = autoFlushEnabled;
        }

        public void flush() throws IOException {
            lock.writeLock().lock();
            try {
                root.flushAndReloadZipFS();
                writes.set(0);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public @NotNull CompressionFormat getCompressionFormat() {
            return format;
        }

        @Override
        public void setCompressionFormat(@Nullable CompressionFormat format) {
            if (format == null)
                format = new CompressionFormat(null, ZipCompressionMethod.DEFLATED);
            this.format = format;
        }

        private void ensureWrite() throws IOException {
            if (!isAutoFlushEnabled() || maxWrites <= 0) // disabled
                return;
            if (writes.incrementAndGet() >= maxWrites) {
                lock.writeLock().lock();
                try {
                    if (writes.get() >= maxWrites) {
                        root.flushAndReloadZipFS();
                        writes.set(0);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        private String relativizeToRoot(ResolvedPath resolvedToSubFs, @Nullable String relativizeTo) {
            lock.readLock().lock();
            try {
                ZipFSTreeNode node = resolvedToSubFs.fs;
                String current = resolvedToSubFs.getPath().toString();
                while (node.parent != null) { //do not execute for root node...
                    current = node.location.resolve(current.substring(1)).toString();
                    node = node.parent;
                }

                String r;
                if (relativizeTo != null) {
                    r = Path.of(relativizeTo).relativize(Path.of(current)).toString();
                } else {
                    r = current.startsWith("/") ? current.substring(1) : current;
                    if (r.isBlank())
                        r = null;
                }
                return r;
            } finally {
                lock.readLock().unlock();
            }
        }

        private boolean isOnCompressedLevel(Path source){
            return Arrays.binarySearch(format.compressionLevels, source.getNameCount() - 1) >= 0;
        }

        private ResolvedPath resolvePath(String relativeFromRoot, boolean isDir) throws IOException {
            ZipFSTreeNode zipFSNode = root;
            lock.updateLock().lock();
            try {
                if (relativeFromRoot != null) {
                    final Path source = zipFSNode.zipFS.getPath(relativeFromRoot);
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
                                                zipFSNode, prefix, zipFSNode.useTempFile, format.getCompression(idx), lock));
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
                    return new ResolvedPath(zipFSNode, (source != prefix ? prefix.relativize(source) : source).toString());
                } else {
                    return new ResolvedPath(zipFSNode, "/"); //root
                }
            } finally {
                lock.updateLock().unlock();
            }
        }

        @Override
        public void writeFile(String relativeFrom, String relativeTo, IOFunctions.BiIOConsumer<Path, Path> writeWithFS) throws IOException {
            try {
                ResolvedPath fsPFrom = null;
                ResolvedPath fsPTo = null;
                lock.updateLock().lock();
                try {
                    fsPFrom = resolvePath(relativeFrom, false); //copy zipped sub fs as file without decompressing it
                    fsPTo = resolvePath(relativeTo, false); //copy zipped sub fs as file without decompressing it
                } finally {
                    lock.updateLock().unlock();
                }

                lock.readLock().lock();
                try {
                    writeWithFS.accept(fsPFrom.getPath(), fsPTo.getPath());
                } finally {
                    lock.readLock().unlock();
                }
                ensureWrite();
            } catch (ClosedFileSystemException | ClosedChannelException e) {
                LoggerFactory.getLogger(getClass()).warn("FS copy operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                LoggerFactory.getLogger(getClass()).debug("FS copy operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                lock.writeLock().lock();
                try {
                    flush();
                    ResolvedPath fsPFrom = resolvePath(relativeFrom, false); //copy zipped sub fs as file without decompressing it
                    ResolvedPath fsPTo = resolvePath(relativeTo, false); //copy zipped sub fs as file without decompressing it
                    writeWithFS.accept(fsPFrom.getPath(), fsPTo.getPath());
                    ensureWrite();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        public void writeFile(String relative, IOFunctions.IOConsumer<Path> writeWithFS) throws IOException {
            try {
                ResolvedPath rp = resolvePath(relative, false);
                lock.readLock().lock();
                try {
                    writeWithFS.accept(rp.getPath());
                } finally {
                    lock.readLock().unlock();
                }
                ensureWrite();
            } catch (ClosedFileSystemException | ClosedChannelException e) {
                LoggerFactory.getLogger(getClass()).warn("FS write operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.");
                LoggerFactory.getLogger(getClass()).debug("FS write operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                lock.writeLock().lock();
                try {
                    flush();
                    ResolvedPath fs = resolvePath(relative, false);
                    writeWithFS.accept(fs.getPath());
                    ensureWrite();
                } finally {
                    lock.writeLock().unlock();
                }
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

        @Override
        public List<String> list(String relative, String globPattern, boolean recursive, boolean includeFiles, boolean includeDirs) throws IOException {
                List<String> output = new ArrayList<>();
                final ResolvedPath fs = resolvePath(relative, true);
                final String walkingRoot = relativizeToRoot(fs, null);

                if (globPattern != null)
                    globPattern = "glob:" + globPattern;

                if (includeDirs && (globPattern == null || fs.fs.zipFS.getPathMatcher(globPattern).matches(fs.getPath())))
                    output.add(relativizeToRoot(fs, walkingRoot));

                final Queue<ResolvedPath> paths;
                lock.readLock().lock();
                try {
                    paths = FileUtils.walkAndClose(w -> w.collect(Collectors.toList()), fs.getPath(), recursive ? Integer.MAX_VALUE : 1, globPattern)
                            .stream().map(p -> new ResolvedPath(fs.fs, p.toString())).collect(Collectors.toCollection(LinkedList::new));
                } finally {
                    lock.readLock().unlock();
                }
                paths.poll(); // remove walk root

                while (!paths.isEmpty()) {
                    final ResolvedPath current = paths.poll();
                    if (Files.isDirectory(current.getPath())) {
                        String relativeFromRoot = relativizeToRoot(current, walkingRoot);
                        if (includeDirs)
                            output.add(relativeFromRoot);
                    } else {
                        final Path currentP = current.getPath();
                        // lazy file ext matching to speedup check for zip files
                        final PathMatcher noZipExt = currentP.getFileSystem().getPathMatcher("glob:**{.ms?, .tsv, .csv, .info, .config}");
                        if (isOnCompressedLevel(currentP) &&
                                !noZipExt.matches(currentP) &&
                                FileUtils.isZipArchive(currentP)) {
                            String relativeFromRoot = relativizeToRoot(current, walkingRoot);
                            if (includeDirs)
                                output.add(relativeFromRoot);

                            if (recursive) {
                                final ResolvedPath nuCurrent = resolvePath(current.getPath().toString(), true);
                                lock.readLock().lock();
                                try {
                                    List<ResolvedPath> nuLevel = FileUtils.walkAndClose(w -> w.map(p -> new ResolvedPath(nuCurrent.fs, p.toString()))
                                            .collect(Collectors.toList()), nuCurrent.getPath(), globPattern);
                                    paths.addAll(nuLevel.subList(1, nuLevel.size() - 1)); //remove walk root
                                } finally {
                                    lock.readLock().unlock();
                                }
                            }
                        } else if (includeFiles) {
                            output.add(relativizeToRoot(current, walkingRoot));
                        }
                    }
                }
                return output;
        }

        private <R> R readFile(String relative, boolean isDir, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            try {
                ResolvedPath fs = resolvePath(relative, isDir);
                lock.readLock().lock();
                try {
                    return readWithFS.apply(fs.getPath());
                } finally {
                    lock.readLock().unlock();
                }
            } catch (ClosedFileSystemException | ClosedChannelException e) {
                LoggerFactory.getLogger(getClass()).warn("FS read operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.");
                LoggerFactory.getLogger(getClass()).debug("FS read operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                lock.writeLock().lock();
                try {
                    flush();
                    ResolvedPath fs = resolvePath(relative, isDir);
                    return readWithFS.apply(fs.getPath());
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        @Override
        public Path getLocation() {
            return root.location;
        }

        @Override
        public Path getRoot() {
            return root.zipFS.getPath(root.zipFS.getSeparator());
        }

        public void close() throws IOException {
            lock.writeLock().lock();
            try {
                root.close();
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
        private boolean firstRun = true;

        @Nullable
        private final ZipFSTreeNode parent;

        private FileSystem zipFS;

        private final Map<Path, ZipFSTreeNode> childFileSystems = new HashMap<>();



        private ZipFSTreeNode(@Nullable ZipFSTreeNode parent, Path location, boolean useTempFile, ZipCompressionMethod compressionMethod, ReentrantReadWriteUpdateLock lock) throws IOException {
            this(lock, parent, location, Files.notExists(location), useTempFile, compressionMethod);
        }

        private ZipFSTreeNode(ReentrantReadWriteUpdateLock lock, @Nullable ZipFSTreeNode parent, Path location, boolean createNew, boolean useTempFile, ZipCompressionMethod compressionMethod) throws IOException {
            this.lock = lock;
            this.location = location;
            this.useTempFile = useTempFile;
            this.compressionMethod = compressionMethod;
            this.parent = parent;
            this.zipFS = FileUtils.asZipFS(location, createNew, useTempFile, compressionMethod);
        }

        *//**
         * @param relative resolves path and caches int as currentPath
         * @return Resolved path or arror if path is not part of this FS
         *//*

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
                        LoggerFactory.getLogger(getClass()).warn("ZipFS seems to be closed unexpectedly! Try Reopen it.");
                        flushAndReloadZipFS();
                    }
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
            } finally {
                lock.writeLock().unlock();
            }
        }

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

                if (!FileSystems.getDefault().equals(zipFS)) //do not try to close default fs --> just in case
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
        private final String path;

        private ResolvedPath(ZipFSTreeNode fs, String path) {
            this.fs = fs;
            this.path = path;
        }

        Path getPath(){
         return fs.resolveCurrentPath(path);
        }
    }*/
}