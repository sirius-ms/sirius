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

package de.unijena.bioinf.storage.blob.memory;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryCacheBlobStorage extends InMemoryBlobStorage {
    private final int maxSize;

    private final Set<String> lastAccessed;
    ReadWriteLock lock = new ReentrantReadWriteLock();

    public InMemoryCacheBlobStorage(String name) {
        this(name, 0);
    }

    public InMemoryCacheBlobStorage(String name, int maxSize) {
        super(name);
        this.maxSize = maxSize;
        this.lastAccessed = Collections.synchronizedSet(new LinkedHashSet<>(maxSize + 1));
    }


    @Override
    public void clear() throws IOException {
        lock.writeLock().lock();
        try {
            super.clear();
            lastAccessed.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected byte[] get(@NotNull String key) {
        lock.readLock().lock();
        try {
            byte[] it = super.get(key);
            if (it != null) {
                lastAccessed.remove(key);
                lastAccessed.add(key);
            }
            return it;
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    protected byte[] put(String key, byte[] value) {
        byte[] old;
        lock.readLock().lock();
        try {
            old = super.put(key, value);
            lastAccessed.remove(key);
            lastAccessed.add(key);
        } finally {
            lock.readLock().unlock();
        }

        if (blobs.size() > maxSize) {
            lock.writeLock().lock();
            try {
                Iterator<String> it = lastAccessed.iterator();
                while (blobs.size() > maxSize) {
                    blobs.remove(it.next());
                    it.remove();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return old;
    }

    @Override
    protected byte[] remove(String key) {
        lock.writeLock().lock();
        try {
            lastAccessed.remove(key);
            return super.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
