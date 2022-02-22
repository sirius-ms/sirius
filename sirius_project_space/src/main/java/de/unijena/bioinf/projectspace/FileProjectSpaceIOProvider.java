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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FileProjectSpaceIOProvider implements ProjectIOProvider<FileProjectSpaceIO, FileProjectSpaceReader, FileProjectSpaceWriter> {

    @NotNull
    protected final FileSystemManager fsManager;

    public FileProjectSpaceIOProvider(@NotNull Path root) {
        this(root, new CompressionFormat(new int[]{1}, ZipCompressionMethod.DEFLATED));
    }

    public FileProjectSpaceIOProvider(@NotNull Path root, @Nullable CompressionFormat format) {
        this(() -> {
            if (Files.exists(root) && !Files.isDirectory(root))
                throw new IllegalArgumentException("Uncompressed Project-Space location must be a directory");
            try {
                return new ZipFSTree(root, false, 1, format == null ? new CompressionFormat(null, ZipCompressionMethod.STORED) : format);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
//            }
        });
    }

    protected FileProjectSpaceIOProvider(@NotNull Supplier<FileSystemManager> locationSupplier) {
        this.fsManager = locationSupplier.get();
    }


    @Override
    public FileProjectSpaceIO newIO(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new FileProjectSpaceIO(fsManager, propertyGetter);
    }

    @Override
    public FileProjectSpaceReader newReader(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new FileProjectSpaceReader(fsManager, propertyGetter);
    }

    @Override
    public FileProjectSpaceWriter newWriter(Function<Class<ProjectSpaceProperty>, Optional<ProjectSpaceProperty>> propertyGetter) {
        return new FileProjectSpaceWriter(fsManager, propertyGetter);
    }

    public Path getLocation() {
        return fsManager.getLocation();
    }

    @Override
    public void flush() throws IOException {
        fsManager.flush();
    }

    public void close() throws IOException {
        fsManager.close();
    }

    @Override
    public CompressionFormat getCompressionFormat() {
        return fsManager.getCompressionFormat();
    }

    @Override
    public void setCompressionFormat(@NotNull CompressionFormat format) {
        fsManager.setCompressionFormat(format);
    }


    static class ZipFSTree implements FileSystemManager {
        private final Path location;
        private boolean autoFlushEnabled = true;

        private final ZipNode root;
        private final Map<Path, ZipNode> childFileSystems = new HashMap<>();
        private final ReentrantReadWriteUpdateLock childFileSystemsLock;

        private final boolean useTempFile;
        private final int maxWrites;

        private CompressionFormat compressionFormat;

        public ZipFSTree(Path location, boolean useTempFile, int maxWrites, @NotNull CompressionFormat format) throws IOException {
            this.location = location;
            this.useTempFile = useTempFile;
            this.maxWrites = maxWrites;
            this.childFileSystemsLock = new ReentrantReadWriteUpdateLock();
            setCompressionFormat(format);
            root = new ZipNode(this, null, location, useTempFile, maxWrites, format.compressionMethod, location.getFileSystem());
        }

        public boolean isAutoFlushEnabled() {
            return autoFlushEnabled;
        }

        public void setAutoFlushEnabled(boolean autoFlushEnabled) {
            this.autoFlushEnabled = autoFlushEnabled;
        }

        private String relativizeToRoot(ResolvedPath resolvedToSubFs, @Nullable String relativizeToSubRoot) {
            childFileSystemsLock.readLock().lock();
            try {
                String current;
                if (resolvedToSubFs.fs.isDefault()) { //is default fs and no archive
//                    relativizeToSubRoot = resolvedToSubFs.fs.location.toString();
                    current = resolvedToSubFs.getPath().toString();
                } else {
                    current = resolvedToSubFs.fs.location.resolve(resolvedToSubFs.getPath().toString().substring(1)).toString();
                }

                String r;
                if (relativizeToSubRoot != null) {
                    r = location.resolve(Path.of(relativizeToSubRoot)).relativize(Path.of(current)).toString();
                } else {
                    r = location.relativize(Path.of(current)).toString()/*current.startsWith("/") ? current.substring(1) : current*/;
                }
                if (r.isBlank())
                    r = null;
                return r;
            } finally {
                childFileSystemsLock.readLock().unlock();
            }
        }

        private boolean isOnCompressedLevel(Path source) {
            return compressionFormat.getCompressedLevel() == source.getNameCount() - 1;
        }

        private ResolvedPath resolvePath(String relativeFromRoot, Boolean isDir) throws IOException {
            if (compressionFormat.getCompressedLevel() < 1)
                return new ResolvedPath(root, root.location.resolve(relativeFromRoot).toString());

            childFileSystemsLock.updateLock().lock();
            try {
                if (relativeFromRoot != null && !relativeFromRoot.isBlank()) {
                    final Path source = location.getFileSystem().getPath(relativeFromRoot);
                    final PathMatcher noZipExt = source.getFileSystem().getPathMatcher("glob:**{.ms, .tsv, .csv, .info, .config}");

                    if ((compressionFormat.getCompressedLevel() >= source.getNameCount())
                            || (isDir != null && !isDir && isOnCompressedLevel(source))
                            || (isDir == null && isOnCompressedLevel(source) && !noZipExt.matches(source) && FileUtils.isZipArchive(source))
                    ) {
                        return new ResolvedPath(root, root.location.resolve(source).toString());
                    } else {
                        Path prefix = source.subpath(0, compressionFormat.getCompressedLevel() + 1);

                        ZipNode zipFSNode = childFileSystems.get(prefix);
                        if (zipFSNode == null) {
                            childFileSystemsLock.writeLock().lock();
                            try {
                                if (!childFileSystems.containsKey(prefix)) {
                                    childFileSystems.put(prefix, new ZipNode(this, location.resolve(prefix), useTempFile, maxWrites, compressionFormat.compressionMethod, new ReentrantReadWriteUpdateLock()));
                                }
                                zipFSNode = childFileSystems.get(prefix);
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
            try {
                ResolvedPath fsPFrom = null;
                ResolvedPath fsPTo = null;

                ResolvedPath[] lockOrder;

                childFileSystemsLock.updateLock().lock();
                try {
                    fsPFrom = resolvePath(relativeFrom, false); //copy zipped sub fs as file without decompressing it
                    fsPTo = resolvePath(relativeTo, false); //copy zipped sub fs as file without decompressing it
                    lockOrder = new ResolvedPath[]{fsPFrom, fsPFrom};
                    Arrays.sort(lockOrder);
                } finally {
                    childFileSystemsLock.updateLock().unlock();
                }


                for (ResolvedPath resolvedPath : lockOrder)
                    if (!resolvedPath.fs.isDefault())
                        resolvedPath.fs.lock.readLock().lock();

                try {
                    writeWithFS.accept(fsPFrom.getPath(), fsPTo.getPath());
                } finally {
                    for (ResolvedPath resolvedPath : lockOrder)
                        if (!resolvedPath.fs.isDefault())
                            resolvedPath.fs.lock.readLock().unlock();
                }

                fsPTo.fs.ensureWrite();
            } catch (ClosedFileSystemException | ClosedChannelException e) {
                LoggerFactory.getLogger(getClass()).warn("FS copy operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                LoggerFactory.getLogger(getClass()).debug("FS copy operation cancelled unexpectedly! Connection to ZipFS Lost. Try reopening and execute with Full Lock.", e);
                //todo rewerite with full lock
                /*childFileSystemsLock.writeLock().lock();
                try {
                    flush();
                    ResolvedPath fsPFrom = resolvePath(relativeFrom, false); //copy zipped sub fs as file without decompressing it
                    ResolvedPath fsPTo = resolvePath(relativeTo, false); //copy zipped sub fs as file without decompressing it
                    writeWithFS.accept(fsPFrom.getPath(), fsPTo.getPath());
                    ensureWrite();
                } finally {
                    childFileSystemsLock.writeLock().unlock();
                }*/
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
                }
                rp.fs.ensureWrite();
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
                }
                rp.fs.ensureWrite();
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
                    rp.fs.ensureWrite();
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
                    rp.fs.ensureWrite();
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
                    workingRootFS.fs.ensureWrite();
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
                    if (!current.fs.isDefault()) {
                        current.fs.lock.readLock().unlock();
                        current.fs.ensureWrite();
                    }
                }
            }
            return output;
        }

        @Override
        public Path getLocation() {
            return location;
        }

        public void flush() throws IOException {
            close();
        }

        @Override
        public CompressionFormat getCompressionFormat() {
            return compressionFormat;
        }

        @Override
        public void setCompressionFormat(CompressionFormat format) {
            if (format.compressionLevels != null && format.compressionLevels.length > 1)
                throw new IllegalArgumentException("MultiLevel compression is not supported for folder based project-space");
            this.compressionFormat = format;
        }


        public void close() throws IOException {
            childFileSystemsLock.updateLock().lock();
            try {
                ArrayList<Map.Entry<Path, ZipNode>> entries = new ArrayList<>(childFileSystems.entrySet());
                for (Map.Entry<Path, ZipNode> entry : entries) {
                    final ZipNode zipfs = entry.getValue();
                    zipfs.close();
                }
            } finally {
                childFileSystemsLock.updateLock().unlock();
            }
        }
    }

    private static class ZipNode implements Closeable, Comparable<ZipNode> {
        private final ReentrantReadWriteUpdateLock lock;
        private final Path location; // the location on the default (real) fs
        private final boolean useTempFile;
        private final ZipCompressionMethod compressionMethod;

        private final int maxWrites;
        private int writes = 0;


        private final ZipFSTree parent;
        private FileSystem zipFS;


        private ZipNode(ZipFSTree parent, Path location, boolean useTempFile, int maxwrites, ZipCompressionMethod compressionMethod, ReentrantReadWriteUpdateLock lock) throws IOException {
            this(parent, lock, location, Files.notExists(location), useTempFile, maxwrites, compressionMethod);
        }

        private ZipNode(ZipFSTree parent, ReentrantReadWriteUpdateLock lock, Path location, boolean createNew, boolean useTempFile, int maxWrites, ZipCompressionMethod compressionMethod) throws IOException {
            this(parent, lock, location, useTempFile, maxWrites, compressionMethod, FileUtils.asZipFS(location, createNew, useTempFile, compressionMethod));
        }

        private ZipNode(ZipFSTree parent, ReentrantReadWriteUpdateLock lock, Path location, boolean useTempFile, int maxWrites, ZipCompressionMethod compressionMethod, FileSystem fs) {
            this.lock = lock;
            this.location = location;
            this.parent = parent;
            this.useTempFile = useTempFile;
            this.compressionMethod = compressionMethod;
            this.maxWrites = maxWrites;
            this.zipFS = fs;
        }

        /**
         * @param path resolves path and caches int as currentPath
         * @return Resolved path or arror if path is not part of this FS
         */

        private Path resolveCurrentPath(String path) {
            if (path == null || path.isBlank() || path.equals("/") || path.equals(zipFS.getSeparator())) {
                if (isDefault())
                    return location;
                else
                    return zipFS.getPath(zipFS.getSeparator());
            } else {
                return zipFS.getPath(path);
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
            if (++writes >= maxWrites) {
                lock.writeLock().lock();
                try {
                    if (writes >= maxWrites)
                        close(); //just close and reopen if needed again to save memory
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        private void ensureOpen() throws IOException {
            if (isDefault())
                return;
            if (!zipFS.isOpen()) {
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
        }

        public void close() throws IOException {
            if (isDefault())
                return;
            lock.writeLock().lock();
            try {
                if (zipFS.isOpen())
                    zipFS.close();
                parent.childFileSystemsLock.writeLock().lock();
                try {
                    parent.childFileSystems.remove(parent.location.relativize(location));
                } finally {
                    parent.childFileSystemsLock.writeLock().unlock();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public int compareTo(@NotNull FileProjectSpaceIOProvider.ZipNode o) {
            return location.compareTo(o.location);
        }
    }

    private static class ResolvedPath implements Comparable<ResolvedPath> {
        private final ZipNode fs;
        private final String path;

        private ResolvedPath(@NotNull ZipNode fs, String path) {
            this.fs = fs;
            this.path = path;
        }

        Path getPath() {
            return fs.resolveCurrentPath(path);
        }


        @Override
        public int compareTo(@NotNull FileProjectSpaceIOProvider.ResolvedPath o) {
            return fs.location.toString()
                    .compareTo(o.fs.location.toString());
        }
    }
}