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


package de.unijena.bioinf.storage.blob.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.Compressible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FileBlobStorage implements BlobStorage {
    public static final String BLOB_TAGS = ".tags";

    public static boolean exists(@Nullable Path p) throws IOException {
        return p != null && Files.isDirectory(p) && Files.list(p).count() > 0;
    }

    public static Compressible.Compression detectCompression(@NotNull Path root) throws IOException {
        return FileUtils.walkAndClose(s -> s.filter(Files::isRegularFile).findFirst().map(Compressible.Compression::fromPath).orElse(Compressible.Compression.NONE), root);
    }


    protected final Path root;

    private Map<String, String> tags = null;
    private final Map<String, ReentrantReadWriteLock> pathLocks = new HashMap<>();
    private final ReadWriteLock pathLocksLock = new ReentrantReadWriteLock();

    public FileBlobStorage(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public String getName() {
        return root.getFileName().toString();
    }

    @Override
    public String getBucketLocation() { //should we use "file://" prefix for consistency
        return root.toAbsolutePath().toString();
    }

    @Override
    public long size() throws IOException {
        return FileUtils.getFolderSize(root);
    }

    @Override
    public @NotNull Map<String, String> getTags() throws IOException {
        Path tagPath = root.resolve(BLOB_TAGS);
        if (tags == null) {
            if (Files.notExists(tagPath))
                tags = new HashMap<>();
            else
                withWriteLock(tagPath, p -> {
                    try (InputStream r = reader(p)) {
                        this.tags = new ObjectMapper().readValue(r, new TypeReference<>() {
                        });
                    }
                });
        }

        return withReadLock(tagPath, p -> Collections.unmodifiableMap(tags));
    }

    @Override
    public void setTags(@NotNull Map<String, String> tags) throws IOException {
        withWriteLock(Path.of(BLOB_TAGS), p -> withWriter(p, o -> new ObjectMapper().writeValue(o, tags)));
    }

    @Override
    public boolean hasBlob(@NotNull Path path) {
        return Files.isRegularFile(root.resolve(path));
    }

    @Override
    public Iterator<Blob> listBlobs() throws IOException {
        return new BlobIt<>(FileUtils.walkAndClose(s ->
                s.filter(p -> !p.getFileName().toString().equals(BLOB_TAGS) && !p.equals(root))
                        .sorted(Comparator.reverseOrder()).collect(Collectors.toList()), getRoot()).iterator(), PathBlob::new);
    }

    public class PathBlob implements Blob {
        final Path source;

        private PathBlob(@NotNull Path source) {
            this.source = source;
        }

        @Override
        public boolean isDirectory() {
            return Files.isDirectory(source);
        }

        @Override
        public String getKey() {
            return getRoot().relativize(source).toString();
        }

        @Override
        public long size() {
            try {
                return Files.size(source);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean deleteBlob(Path relative) throws IOException {
        return withWriteLockR(root.resolve(relative), Files::deleteIfExists);
    }

    @Override
    public void deleteBucket() throws IOException {
        try {
            if (Files.notExists(root))
                return;

            List<Path> files = FileUtils.walkAndClose(w -> w.sorted(Comparator.reverseOrder()).collect(Collectors.toList()), root);
            pathLocksLock.writeLock().lock();
            try {
                for (Path file : files)
                    withWriteLock(file, Files::deleteIfExists);
            } finally {
                pathLocksLock.writeLock().unlock();
            }
        } finally {
            close();
        }
    }

    @Override
    public @Nullable InputStream reader(@NotNull Path relative) throws IOException {
        Path blob = root.resolve(relative);
        if (!Files.isRegularFile(blob))
            return null;
        return new LockedInputStream(blob.toFile());
    }

    //
    protected OutputStream writer(Path relative) throws IOException {
        @NotNull Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        return new LockedOutputStream(target.toFile());
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        try (OutputStream w = writer(relative)) {
            withStream.accept(w);
        }
    }


    private <R> R withReadLock(@NotNull final Path absolute, @NotNull final IOFunctions.IOFunction<Path, R> doWith) throws IOException {
        final String abs = absolute.toAbsolutePath().toString();
        final ReentrantReadWriteLock lock = readLockPath(abs);
        try {
            return doWith.apply(absolute);
        } finally {
            readUnLockPath(abs, lock);
        }
    }

    private ReentrantReadWriteLock readLockPath(String absolute) {
        pathLocksLock.readLock().lock();
        try {
            ReentrantReadWriteLock lock = pathLocks.get(absolute);
            if (lock != null) {
                lock.readLock().lock();
                return lock;
            }
        } finally {
            pathLocksLock.readLock().unlock();
        }

        pathLocksLock.writeLock().lock();
        try {
            ReentrantReadWriteLock lock = pathLocks.computeIfAbsent(absolute, path -> new ReentrantReadWriteLock());
            lock.readLock().lock();
            return lock;
        } finally {
            pathLocksLock.writeLock().unlock();
        }
    }

    private void readUnLockPath(@NotNull String absolute, @NotNull ReentrantReadWriteLock lock) {
        lock.readLock().unlock();
        removeLockIfFree(absolute, lock);
    }

    private void withWriteLock(@NotNull final Path absolute, @NotNull final IOFunctions.IOConsumer<Path> doWith) throws IOException {
        final String abs = absolute.toAbsolutePath().toString();
        final ReentrantReadWriteLock lock = writeLockPath(abs);
        try {
            doWith.accept(absolute);
        } finally {
            writeUnLockPath(abs, lock);
        }
    }

    private <R> R withWriteLockR(@NotNull final Path absolute, @NotNull final IOFunctions.IOFunction<Path, R> doWith) throws IOException {
        final String abs = absolute.toAbsolutePath().toString();
        final ReentrantReadWriteLock lock = writeLockPath(abs);
        try {
            return doWith.apply(absolute);
        } finally {
            writeUnLockPath(abs, lock);
        }
    }

    private ReentrantReadWriteLock writeLockPath(String absolute) {
        pathLocksLock.readLock().lock();
        try {
            ReentrantReadWriteLock lock = pathLocks.get(absolute);
            if (lock != null) {
                lock.writeLock().lock();
                return lock;
            }
        } finally {
            pathLocksLock.readLock().unlock();
        }

        pathLocksLock.writeLock().lock();
        try {
            ReentrantReadWriteLock lock = pathLocks.computeIfAbsent(absolute, path -> new ReentrantReadWriteLock());
            lock.writeLock().lock();
            return lock;
        } finally {
            pathLocksLock.writeLock().unlock();
        }
    }

    private void writeUnLockPath(@NotNull String absolute, @NotNull ReentrantReadWriteLock lock) {
        lock.writeLock().unlock();
        removeLockIfFree(absolute, lock);
    }

    private void removeLockIfFree(@NotNull String absolute, @NotNull ReentrantReadWriteLock lock) {
        if (lock == pathLocks.get(absolute)) {
            if (!lock.isWriteLocked() && lock.getReadLockCount() == 0 && !lock.hasQueuedThreads()) {
                try {
                    pathLocksLock.writeLock().lock();
                    if (!lock.isWriteLocked() && lock.getReadLockCount() == 0 && !lock.hasQueuedThreads())
                        pathLocks.remove(absolute);
//                                }
                } finally {
                    pathLocksLock.writeLock().unlock();
                }
            }
        } else {
            LoggerFactory.getLogger(getClass()).warn("Lock for path '" + absolute + "' does not exist or differs!");
        }
    }

    /**
     * Write locks the given Path and frees the lock when closed
     */
    public final class LockedOutputStream extends FileOutputStream {
        private ReentrantReadWriteLock lock;
        private final String path;

        private LockedOutputStream(@NotNull File file) throws FileNotFoundException {
            super(file);
            path = file.getAbsolutePath();
            lock = writeLockPath(path);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (lock != null) {
                    writeUnLockPath(path, lock);
                    lock = null;
                }
            }
        }
    }

    /**
     * Read locks the given Path and frees the lock when closed
     */
    public final class LockedInputStream extends FileInputStream {
        private ReentrantReadWriteLock lock;
        private final String path;

        private LockedInputStream(@NotNull File file) throws FileNotFoundException {
            super(file);
            path = file.getAbsolutePath();
            lock = readLockPath(path);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (lock != null) {
                    readUnLockPath(path, lock);
                    lock = null;
                }
            }
        }
    }
}
