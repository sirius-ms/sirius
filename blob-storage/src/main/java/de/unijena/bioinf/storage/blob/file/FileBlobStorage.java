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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private final ReadWriteLock tagLock = new ReentrantReadWriteLock();
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
        if (tags == null) {
            tagLock.writeLock().lock();
            try (InputStream r = reader(Path.of(BLOB_TAGS))) {
                this.tags = new ObjectMapper().readValue(r, new TypeReference<>() {
                });
            } finally {
                tagLock.writeLock().unlock();
            }
        }
        tagLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(tags);
        } finally {
            tagLock.readLock().unlock();
        }
    }

    @Override
    public void setTags(@NotNull Map<String, String> tags) throws IOException {
        tagLock.writeLock().lock();
        try {
            this.tags = tags;
            withWriter(Path.of(BLOB_TAGS), o -> new ObjectMapper().writeValue(o, tags));
        } finally {
            tagLock.writeLock().unlock();
        }

    }

    @Override
    public boolean hasBlob(@NotNull Path path) {
        return Files.isRegularFile(root.resolve(path));
    }

    @Override
    public Iterator<Blob> listBlobs() throws IOException {
        return new BlobIt<>(FileUtils.walkAndClose(s -> s.filter(p -> !p.getFileName().toString().equals(BLOB_TAGS)).collect(Collectors.toList()), getRoot()).iterator(), PathBlob::new);
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
        return Files.deleteIfExists(root.resolve(relative));
    }

    @Override
    public void deleteBucket() throws IOException {
        try {
            FileUtils.deleteRecursively(root);
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


   /* private void readLockPath(String absolute) {
        pathLocksLock.readLock().lock();
        try {
            ReadWriteLock lock = pathLocks.get(absolute);
            if (lock != null) {
                lock.readLock().lock();
                return;
            }
        } finally {
            pathLocksLock.readLock().unlock();
        }

        pathLocksLock.writeLock().lock();
        try {
            pathLocks.computeIfAbsent(absolute, path -> new ReentrantReadWriteLock()).readLock().lock();
        } finally {
            pathLocksLock.writeLock().unlock();
        }
    }

    private void readUnLockPath(String absolute) {
        pathLocksLock.readLock().lock();
        ReentrantReadWriteLock lock = null;
        try {
            lock = pathLocks.get(absolute);
            if (lock == null) {
                LoggerFactory.getLogger(getClass()).warn("Lock for path '" + absolute + "' does not exist!");
                return;
            } else {
                lock.readLock().unlock();
                if (lock.isWriteLocked() || lock.getReadLockCount() > 0)
                    return;
            }
        } finally {
            pathLocksLock.readLock().unlock();
        }

        pathLocksLock.writeLock().lock();
        try {
            if (!lock.isWriteLocked() && lock.getReadLockCount() == 0)
                pathLocks.remove(absolute);
        } finally {
            pathLocksLock.writeLock().unlock();
        }
    }

    private void writeLockPath(String absolute) {
        pathLocksLock.readLock().lock();
        try {
            ReadWriteLock lock = pathLocks.get(absolute);
            if (lock != null) {
                lock.writeLock().lock();
                return;
            }
        } finally {
            pathLocksLock.readLock().unlock();
        }

        pathLocksLock.writeLock().lock();
        try {
            pathLocks.computeIfAbsent(absolute, path -> new ReentrantReadWriteLock()).writeLock().lock();
        } finally {
            pathLocksLock.writeLock().unlock();
        }
    }

    private void writeUnLockPath(String absolute) {
        pathLocksLock.readLock().lock();
        ReentrantReadWriteLock lock = null;
        try {
            lock = pathLocks.get(absolute);
            if (lock == null) {
                LoggerFactory.getLogger(getClass()).warn("Lock for path '" + absolute + "' does not exist!");
                return;
            } else {
                lock.writeLock().unlock();
                if (lock.isWriteLocked() || lock.getReadLockCount() > 0)
                    return;
            }
        } finally {
            pathLocksLock.readLock().unlock();
        }

        pathLocksLock.writeLock().lock();
        try {
            if (!lock.isWriteLocked() && lock.getReadLockCount() == 0)
                pathLocks.remove(absolute);
        } finally {
            pathLocksLock.writeLock().unlock();
        }
    }*/

    /**
     * Write locks the given Path and frees the lock when closed
     */
    public final class LockedOutputStream extends FileOutputStream {
        private ReentrantReadWriteLock lock;
        private final String path;

        private LockedOutputStream(@NotNull File file) throws FileNotFoundException {
            super(file);
            path = file.getAbsolutePath();

            pathLocksLock.readLock().lock();
            try {
//                synchronized (pathLocks) {
                lock = pathLocks.get(path);
                if (lock != null)
                    lock.writeLock().lock();
//                }
            } finally {
                pathLocksLock.readLock().unlock();
            }

            if (lock == null) {
                pathLocksLock.writeLock().lock();
                try {
                    //                synchronized (pathLocks) {
                    lock = pathLocks.computeIfAbsent(path, r -> new ReentrantReadWriteLock());
                    lock.writeLock().lock();
                    //                }
                } finally {
                    pathLocksLock.writeLock().unlock();
                }
            }
//            System.out.println(file.getAbsolutePath() + ": Waiter=" + lock.getReadLockCount() + ":" + lock.isWriteLocked());
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (lock != null) {
                    lock.writeLock().unlock();
                    if (lock == pathLocks.get(path)) {
                        if (!lock.isWriteLocked() && lock.getReadLockCount() == 0 && !lock.hasQueuedThreads()) {
                            try {
                                pathLocksLock.writeLock().lock();
//                                synchronized (pathLocks) {
                                if (!lock.isWriteLocked() && lock.getReadLockCount() == 0 && !lock.hasQueuedThreads())
                                    pathLocks.remove(path);
//                                }
                            } finally {
                                pathLocksLock.writeLock().unlock();
                            }
                        }
                    }
                    lock = null;
//                    System.out.println("Number of locks=" + pathLocks.size());
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
            pathLocksLock.readLock().lock();
            try {
//                synchronized (pathLocks) {
                lock = pathLocks.get(path);
                if (lock != null)
                    lock.readLock().lock();
//                }
            } finally {
                pathLocksLock.readLock().unlock();
            }

            if (lock == null) {
                pathLocksLock.writeLock().lock();
                try {
                    //                synchronized (pathLocks) {
                    lock = pathLocks.computeIfAbsent(path, r -> new ReentrantReadWriteLock());
                    lock.readLock().lock();
                    //                }
                } finally {
                    pathLocksLock.writeLock().unlock();
                }
            }
//            System.out.println(file.getAbsolutePath() + ": Waiter=" + lock.getReadLockCount() + ":" + lock.isWriteLocked());
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (lock != null) {
                    lock.readLock().unlock();
                    if (lock == pathLocks.get(path)) {
                        if (!lock.isWriteLocked() && lock.getReadLockCount() == 0 && !lock.hasQueuedThreads()) {
                            pathLocksLock.writeLock().lock();
                            try {
//                                synchronized (pathLocks) {
                                if (!lock.isWriteLocked() && lock.getReadLockCount() == 0 && !lock.hasQueuedThreads())
                                    pathLocks.remove(path);
//                                }
                            } finally {
                                pathLocksLock.writeLock().unlock();
                            }
                        }
                    }
                    lock = null;
//                    System.out.println("Number of locks=" + pathLocks.size());
                }
            }
        }
    }
}
