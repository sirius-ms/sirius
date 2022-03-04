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
import java.nio.channels.ClosedChannelException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PathProjectSpaceIOProvider implements ProjectIOProvider<PathProjectSpaceIO, PathProjectSpaceReader, PathProjectSpaceWriter> {

    @NotNull
    protected final FileSystemManager fsManager;

    public PathProjectSpaceIOProvider(@NotNull Path root) {
        this(root, new CompressionFormat(new int[]{1}, ZipCompressionMethod.DEFLATED));
    }

    public PathProjectSpaceIOProvider(@NotNull Path root, @Nullable CompressionFormat format) {
        this(() -> {
            if (Files.exists(root) && !Files.isDirectory(root))
                throw new IllegalArgumentException("Uncompressed Project-Space location must be a directory");
            try {
                return new FSTree(root, false,
                        PropertyManager.getInteger("de.unijena.bioinf.sirius.pathfs.maxWritesBeforeFlush", 25),
                        PropertyManager.getInteger("de.unijena.bioinf.sirius.pathfs.subFsBufferSize", 125),
                        format == null ? new CompressionFormat(null, ZipCompressionMethod.STORED) : format);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
//            }
        });
    }

    protected PathProjectSpaceIOProvider(@NotNull Supplier<FileSystemManager> locationSupplier) {
        this.fsManager = locationSupplier.get();
    }


    @Override
    public PathProjectSpaceIO newIO(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new PathProjectSpaceIO(fsManager, propertyGetter);
    }

    @Override
    public PathProjectSpaceReader newReader(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new PathProjectSpaceReader(fsManager, propertyGetter);
    }

    @Override
    public PathProjectSpaceWriter newWriter(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new PathProjectSpaceWriter(fsManager, propertyGetter);
    }

    public Path getLocation() {
        return fsManager.getLocation();
    }

    public Path getRoot() {
        return fsManager.getRoot();
    }

    @Override
    public void flush() throws IOException {
        fsManager.flush();
    }

    public void close() throws IOException {
        fsManager.close();
    }

    @Override
    public @NotNull CompressionFormat getCompressionFormat() {
        return fsManager.getCompressionFormat();
    }

    @Override
    public void setCompressionFormat(@NotNull CompressionFormat format) {
        fsManager.setCompressionFormat(format);
    }


    static class FSTree implements FileSystemManager {
        private final Path location;

        private final boolean useTempFile;
        private final int maxWrites;
        private final int buffersize;

        private CompressionFormat compressionFormat;

        private final FSNode root;

        private final Map<Path, FSNode> childFileSystems;
        private final ReentrantReadWriteUpdateLock childFileSystemsLock;


        public FSTree(Path location, boolean useTempFile, int maxWrites, int subFSBufferSize, @NotNull CompressionFormat format) throws IOException {
            this(location, useTempFile, maxWrites, subFSBufferSize, format, false);
        }

        public FSTree(Path location, boolean useTempFile, int maxWrites, int subFSBufferSize, @NotNull CompressionFormat format, boolean rootAsArchive) throws IOException {
            this.location = location;
            this.useTempFile = useTempFile;
            this.maxWrites = maxWrites;
            this.buffersize = subFSBufferSize;
            this.childFileSystems = new HashMap<>(buffersize + 2);
            this.childFileSystemsLock = new ReentrantReadWriteUpdateLock();
            setCompressionFormat(format);
            if (rootAsArchive)
                root = new FSNode(null, new ReentrantReadWriteLock(), location, useTempFile, maxWrites, format.compressionMethod, FileUtils.asZipFS(location, Files.notExists(location), useTempFile, getCompressionFormat().getRootCompression()));
            else
                root = new FSNode(null, null, location, useTempFile, maxWrites, format.compressionMethod, location.getFileSystem());
        }

        //needs write lock
        private FSNode putFS(Path path, FSNode fs) {
            fs.updateAccess();
            return childFileSystems.put(path, fs);
        }

        //needs write lock
        private FSNode removeFS(Path path) {
            return childFileSystems.remove(path);
        }

        //needs read lock
        private FSNode getFS(Path path) {
            FSNode it = childFileSystems.get(path);
            if (it != null)
                it.updateAccess();
            return it;
        }

        //needs writelock
        private void maintainBuffer() {
            if (childFileSystems.size() > buffersize) {
                int remove = Math.max(1, (int) (buffersize * 0.2));
                childFileSystems.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getValue().lastAccessed.longValue()))
                        .map(Map.Entry::getValue).limit(remove)
                        .forEach(v -> {
                            try {
                                v.close();
                            } catch (IOException e) {
                                LoggerFactory.getLogger(getClass()).warn("Error when closing subfs '" + v.location + "'. Try to Ignore!", e);
                            }
                        });
            }
        }

        private String relativizeToRoot(ResolvedPath resolvedToSubFs, @Nullable String subRoot) {
            if (!resolvedToSubFs.fs.isDefault())
                resolvedToSubFs.fs.lock.readLock().lock();
            try {
                // create absolute path
                String current;
                if (resolvedToSubFs.fs.isDefault()) { //is default fs and no archive
                    current = resolvedToSubFs.getPath().toString();
                } else {
                    current = root.resolveCurrentPath(resolvedToSubFs.fs.location.toString()).resolve(
                            resolvedToSubFs.getPath().toString().substring(1)).toString();
                }

                //relativize to root
                if (current.startsWith(root.location.toString()))
                    current = current.substring(root.location.toString().length());

                if (current.startsWith("/"))
                    current = current.substring(1); //remove leading /

                if (subRoot != null && current.startsWith(subRoot))
                    current = current.substring(subRoot.length());

                if (current.startsWith("/"))
                    current = current.substring(1); //remove leading /

                if (current.isBlank())
                    return null;

                return current;
            } finally {
                if (!resolvedToSubFs.fs.isDefault())
                    resolvedToSubFs.fs.lock.readLock().unlock();
            }
        }

        private boolean isOnCompressedLevel(Path source) {
            return compressionFormat.getCompressedLevel() == source.getNameCount() - 1;
        }

        private ResolvedPath resolvePath(String relativeFromRoot, Boolean isDir) throws IOException {
            if (compressionFormat.getCompressedLevel() < 1)
                return new ResolvedPath(root, relativeFromRoot == null ? null : root.rootPath().resolve(relativeFromRoot).toString());

            childFileSystemsLock.updateLock().lock();
            try {
                if (relativeFromRoot != null && !relativeFromRoot.isBlank()) {
                    final Path source = root.resolveCurrentPath(relativeFromRoot);
                    final PathMatcher noZipExt = source.getFileSystem().getPathMatcher("glob:**{.ms, .tsv, .csv, .info, .config}");

                    if ((compressionFormat.getCompressedLevel() >= source.getNameCount())
                            || (isDir != null && !isDir && isOnCompressedLevel(source))
                            || (isDir == null && isOnCompressedLevel(source) && !noZipExt.matches(source) && FileUtils.isZipArchive(source))
                    ) {
                        return new ResolvedPath(root, root.rootPath().resolve(source).toString());
                    } else {
                        Path prefix = source.subpath(0, compressionFormat.getCompressedLevel() + 1);

                        FSNode zipFSNode = getFS(prefix);
                        if (zipFSNode == null) {
                            childFileSystemsLock.writeLock().lock();
                            try {
                                zipFSNode = new FSNode(this, root.rootPath().resolve(prefix.toString()), useTempFile, maxWrites, compressionFormat.compressionMethod, new ReentrantReadWriteLock());
                                putFS(prefix, zipFSNode);
                                maintainBuffer();
                            } finally {
                                childFileSystemsLock.writeLock().unlock();
                            }
                        } else if (!zipFSNode.zipFS.isOpen()) {
                            zipFSNode.ensureOpen();
                        }

                        return new ResolvedPath(zipFSNode, "/" + (source != prefix ? prefix.relativize(source) : source));
                    }
                } else {
                    return new ResolvedPath(root, null); //root
                }
            } finally {
                childFileSystemsLock.updateLock().unlock();
            }
        }

        @Override
        public void writeFile(String relativeFrom, String relativeTo, IOFunctions.BiIOConsumer<Path, Path> writeWithFS) throws IOException {
            ResolvedPath fsPFrom = null;
            ResolvedPath fsPTo = null;

            ResolvedPath[] lockOrder = null;
            try {
                childFileSystemsLock.updateLock().lock();
                try {
                    fsPFrom = resolvePath(relativeFrom, false); //copy zipped sub fs as file without decompressing it
                    fsPTo = resolvePath(relativeTo, false); //copy zipped sub fs as file without decompressing it
                    lockOrder = new ResolvedPath[]{fsPFrom, fsPFrom};
                    Arrays.sort(lockOrder);
                } finally {
                    childFileSystemsLock.updateLock().unlock();
                }


                for (int i = 0; i < lockOrder.length; i++) {
                    ResolvedPath resolvedPath = lockOrder[i];
                    if (!resolvedPath.fs.isDefault())
                        resolvedPath.fs.lock.readLock().lock();
                }

                try {
                    writeWithFS.accept(fsPFrom.getPath(), fsPTo.getPath());
                } finally {
                    for (int i = lockOrder.length - 1; i >= 0; i--) {
                        ResolvedPath resolvedPath = lockOrder[i];
                        if (!resolvedPath.fs.isDefault())
                            resolvedPath.fs.lock.readLock().unlock();
                    }
                    fsPTo.fs.ensureWrite();
                }
            } catch (ClosedFileSystemException | ClosedChannelException e) {
                LoggerFactory.getLogger(getClass()).warn("FS copy operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.");
                LoggerFactory.getLogger(getClass()).debug("FS copy operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                if (lockOrder != null) {
                    for (int i = 0; i < lockOrder.length; i++) {
                        ResolvedPath resolvedPath = lockOrder[i];
                        if (!resolvedPath.fs.isDefault())
                            resolvedPath.fs.lock.writeLock().lock();
                    }

                    try {
                        fsPFrom.fs.ensureOpen();
                        fsPTo.fs.ensureOpen();
                        writeWithFS.accept(fsPFrom.getPath(), fsPTo.getPath());
                    } finally {
                        for (int i = lockOrder.length - 1; i >= 0; i--) {
                            ResolvedPath resolvedPath = lockOrder[i];
                            if (!resolvedPath.fs.isDefault())
                                resolvedPath.fs.lock.writeLock().unlock();
                        }
                        fsPTo.fs.ensureWrite();
                    }
                } else {
                    throw e;
                }
            }
        }

        public void writeFile(String relative, IOFunctions.IOConsumer<Path> writeWithFS) throws IOException {
            final ResolvedPath rp = resolvePath(relative, false);

            try {
                if (!rp.fs.isDefault())
                    rp.fs.lock.readLock().lock();
                try {
                    writeWithFS.accept(rp.getPath());
                } finally {
                    if (!rp.fs.isDefault())
                        rp.fs.lock.readLock().unlock();
                    rp.fs.ensureWrite();
                }
            } catch (ClosedFileSystemException | ClosedChannelException e) {
                LoggerFactory.getLogger(getClass()).warn("FS write operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.");
                LoggerFactory.getLogger(getClass()).debug("FS write operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                if (!rp.fs.isDefault())
                    rp.fs.lock.writeLock().lock();
                try {
                    rp.fs.ensureOpen();
                    writeWithFS.accept(rp.getPath());
                } finally {
                    if (!rp.fs.isDefault())
                        rp.fs.lock.writeLock().unlock();
                    rp.fs.ensureWrite();
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

        private <R> R readFile(String relative, boolean isDir, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            ResolvedPath rp = resolvePath(relative, isDir);
            try {
                if (!rp.fs.isDefault())
                    rp.fs.lock.readLock().lock();
                try {
                    return readWithFS.apply(rp.getPath());
                } finally {
                    if (!rp.fs.isDefault())
                        rp.fs.lock.readLock().unlock();
                }
            } catch (ClosedFileSystemException | ClosedChannelException e) {
                LoggerFactory.getLogger(getClass()).warn("FS read operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.");
                LoggerFactory.getLogger(getClass()).debug("FS read operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                if (!rp.fs.isDefault())
                    rp.fs.lock.writeLock().lock();
                try {
                    rp.fs.ensureOpen();
                    return readWithFS.apply(rp.getPath());
                } finally {
                    if (!rp.fs.isDefault())
                        rp.fs.lock.writeLock().unlock();
                }

            }
        }

        @Override
        public <R> R withDir(@Nullable String relative, IOFunctions.IOFunction<Path, R> readWithFS) throws IOException {
            return readFile(relative, true, readWithFS);
        }

        @Override
        public List<String> list(String relative, String globPattern, boolean recursive, boolean includeFiles, boolean includeDirs) throws IOException {
            List<String> output = new ArrayList<>();
            final ResolvedPath workingRootFS = resolvePath(relative, true);
            final String walkingRoot = relativizeToRoot(workingRootFS, null);

            if (globPattern != null)
                globPattern = "glob:" + globPattern;

            if (includeDirs && (globPattern == null || workingRootFS.fs.zipFS.getPathMatcher(globPattern).matches(workingRootFS.getPath())))
                output.add(relativizeToRoot(workingRootFS, walkingRoot));

            final Queue<String> paths;

            if (!workingRootFS.fs.isDefault())
                workingRootFS.fs.lock.readLock().lock();
            try {
                paths = FileUtils.walkAndClose(w -> w
                                .filter(p -> !p.equals(workingRootFS.getPath()))
                                .map(p -> relativizeToRoot(new ResolvedPath(workingRootFS.fs, p.toString()), null))
                                .collect(Collectors.toCollection(LinkedList::new)),
                        workingRootFS.getPath(), 1, globPattern);
            } finally {
                if (!workingRootFS.fs.isDefault()) {
                    workingRootFS.fs.lock.readLock().unlock();
                }
            }

            while (!paths.isEmpty()) {
                final ResolvedPath current = resolvePath(paths.poll(), null);
                if (!current.fs.isDefault())
                    current.fs.lock.readLock().lock();
                try {
                    if (Files.isDirectory(current.getPath())) {
                        if (includeDirs) {
                            String relativeFromRoot = relativizeToRoot(current, walkingRoot);
                            output.add(relativeFromRoot);
                        }

                        if (recursive) {
                            List<String> nuLevel = FileUtils.walkAndClose(w -> w
                                    .filter(p -> !p.equals(current.getPath())) //remove walk root
                                    .map(p -> relativizeToRoot(new ResolvedPath(current.fs, p.toString()), null))
                                    .collect(Collectors.toList()), current.getPath(), 1, globPattern);
                            paths.addAll(nuLevel);
                        }
                    } else if (includeFiles) {
                        output.add(relativizeToRoot(current, walkingRoot));
                    }
                } finally {
                    if (!current.fs.isDefault())
                        current.fs.lock.readLock().unlock();
                }
            }
            return output;
        }

        @Override
        public Path getLocation() {
            return location;
        }

        @Override
        public Path getRoot() {
            return root.rootPath();
        }

        public void flush() throws IOException {
            childFileSystemsLock.updateLock().lock();
            try {
                ArrayList<Map.Entry<Path, FSNode>> entries = new ArrayList<>(childFileSystems.entrySet());
                for (Map.Entry<Path, FSNode> entry : entries) {
                    final FSNode zipfs = entry.getValue();
                    zipfs.close();
                }
                if (!root.isDefault()) { //close and reopen root in case it is an archive
                    childFileSystemsLock.writeLock().lock();
                    try {
                        root.close();
                        root.reopen();
                    } finally {
                        childFileSystemsLock.writeLock().unlock();
                    }
                }
            } finally {
                childFileSystemsLock.updateLock().unlock();
            }
        }

        @Override
        public @NotNull CompressionFormat getCompressionFormat() {
            return compressionFormat;
        }

        @Override
        public void setCompressionFormat(@Nullable CompressionFormat format) {
            if (format == null) {
                if (root.isDefault())
                    format = new CompressionFormat(null, ZipCompressionMethod.STORED);
                else
                    format = new CompressionFormat(null, ZipCompressionMethod.DEFLATED);
            }

            if (format.compressionLevels != null && format.compressionLevels.length > 1)
                throw new IllegalArgumentException("MultiLevel compression is not supported for folder based project-space");
            this.compressionFormat = format;
        }


        public void close() throws IOException {
            childFileSystemsLock.updateLock().lock();
            try {
                ArrayList<Map.Entry<Path, FSNode>> entries = new ArrayList<>(childFileSystems.entrySet());
                for (Map.Entry<Path, FSNode> entry : entries) {
                    final FSNode zipfs = entry.getValue();
                    zipfs.close();
                }

                if (!root.isDefault()) {//close and reopen root in case it is an archive
                    childFileSystemsLock.writeLock().lock();
                    try {
                        root.close();
                    } finally {
                        childFileSystemsLock.writeLock().unlock();
                    }
                }
            } finally {
                childFileSystemsLock.updateLock().unlock();
            }
        }
    }

    private static class FSNode implements Closeable, Comparable<FSNode> {
        private final ReentrantReadWriteLock lock;

        private final Path location; // the location on the default (real) fs
        private final boolean useTempFile;
        private final ZipCompressionMethod compressionMethod;

        private final int maxWrites;
        private final AtomicInteger writes = new AtomicInteger(0);

        private final AtomicLong lastAccessed = new AtomicLong(System.currentTimeMillis());

        private final FSTree parent;
        private FileSystem zipFS;


        private FSNode(FSTree parent, Path location, boolean useTempFile, int maxwrites, ZipCompressionMethod compressionMethod, ReentrantReadWriteLock lock) throws IOException {
            this(parent, lock, location, Files.notExists(location), useTempFile, maxwrites, compressionMethod);
        }

        private FSNode(FSTree parent, ReentrantReadWriteLock lock, Path location, boolean createNew, boolean useTempFile, int maxWrites, ZipCompressionMethod compressionMethod) throws IOException {
            this(parent, lock, location, useTempFile, maxWrites, compressionMethod, FileUtils.asZipFS(location, createNew, useTempFile, compressionMethod));
        }

        private FSNode(FSTree parent, ReentrantReadWriteLock lock, Path location, boolean useTempFile, int maxWrites, ZipCompressionMethod compressionMethod, FileSystem fs) {
            this.lock = lock;
            this.location = location;
            this.parent = parent;
            this.useTempFile = useTempFile;
            this.compressionMethod = compressionMethod;
            this.maxWrites = maxWrites;
            this.zipFS = fs;
        }

        private void updateAccess() {
            lastAccessed.set(System.currentTimeMillis());
        }

        /**
         * @param absolute resolves path
         * @return Resolved path or error if path is not part of this FS
         */

        private Path resolveCurrentPath(String absolute) {
            if (absolute == null || absolute.isBlank() || absolute.equals("/") || absolute.equals(zipFS.getSeparator())) {
                if (isDefault()) return location;
                else return zipFS.getPath(zipFS.getSeparator());
            } else {
                return zipFS.getPath(absolute);
            }
        }

        boolean isDefault() {
            return FileSystems.getDefault().equals(zipFS);
        }

        private void ensureWrite() throws IOException {
            if (isDefault())
                return;
            if (maxWrites <= 0) // disabled
                return;
            if (writes.incrementAndGet() >= maxWrites) {
                lock.writeLock().lock();
                try {
                    if (writes.get() >= maxWrites)
                        reopen(); //just close to save memory. can be reopened if needed again.
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        private void ensureOpen() throws IOException {
            if (isDefault())
                return;
            if (!zipFS.isOpen()) {
                reopen();
            }
        }

        //unchecked
        private void reopen() throws IOException {
            try {
                lock.writeLock().lock();
                if (!zipFS.isOpen()) {
                    LoggerFactory.getLogger(getClass()).warn("ZipFS seems to be closed unexpectedly! Try Reopen it.");
                    try {
                        zipFS.close();
                        //            } catch (ClosedChannelException e) {
                        //                LoggerFactory.getLogger(getClass()).error("Could not close ZipFS due to ClosedChannelException usually caused by a thread level interrupt. Try to delete lock and reopen!", e);
                        //                ((ZipFileSystemProvider)zipFS.provider()).removeFileSystem(location, (ZipFileSystem) zipFS); //todo find out how to access  api via gradle
                    } catch (IOException e) {
                        LoggerFactory.getLogger(getClass()).error("Could not close ZipFS. Try to ignore and reopen!", e);
                    }
                    zipFS = FileUtils.asZipFS(location, false, useTempFile, compressionMethod);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }


        public void close() throws IOException {
            if (isDefault())
                return;
            lock.writeLock().lock();
            try {
                if (zipFS.isOpen())
                    zipFS.close();
                if (parent != null) {
                    parent.childFileSystemsLock.writeLock().lock();
                    try {
                        parent.childFileSystems.remove(parent.root.rootPath().relativize(location));
                    } finally {
                        parent.childFileSystemsLock.writeLock().unlock();
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public int compareTo(@NotNull PathProjectSpaceIOProvider.FSNode o) {
            return location.compareTo(o.location);
        }

        private Path rootPath() {
            return resolveCurrentPath(null);
        }
    }

    private static class ResolvedPath implements Comparable<ResolvedPath> {
        private final FSNode fs;
        private final String path;

        private ResolvedPath(@NotNull PathProjectSpaceIOProvider.FSNode fs, String path) {
            this.fs = fs;
            this.path = path;
        }

        Path getPath() {
            return fs.resolveCurrentPath(path);
        }

        @Override
        public int compareTo(@NotNull PathProjectSpaceIOProvider.ResolvedPath o) {
            return fs.location.toString()
                    .compareTo(o.fs.location.toString());
        }
    }
}