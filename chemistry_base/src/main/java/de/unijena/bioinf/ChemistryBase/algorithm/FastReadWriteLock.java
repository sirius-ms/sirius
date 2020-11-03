
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.algorithm;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Very fast Read/Write Lock implementation. Reaches nearly the same performance as unsynchronized code
 * if only read operations are performed. But also for rarely write operations the performance is magnitude
 * faster than javas Read/Write lock implementation. This implementation expects that write operations are
 * done rarely and that read and write operations take very short time.
 * If a read or write operation is time consuming (e.g. because it reads/writes from
 * file or network) you should consider java's read/write lock implementation.
 *
 *  FastReadWriteLock lock = new FastReadWriteLock();
 *
 *  // reading code
 *  while (true) {
 *      final long locked = lock.startReading();
 *      // your reading code
 *      if (!lock.canFinishReading(locked)) break;
 *  }
 *
 *  // writing code
 *  lock.startWriting();
 *  // writing code
 *  lock.finishWriting();
 *
 */
public class FastReadWriteLock {

    private final AtomicLong counter;
    private final ReentrantLock lock;

    public FastReadWriteLock() {
        this.counter = new AtomicLong();
        this.lock = new ReentrantLock();
    }

    public void startWriting() {
        lock.lock();
        counter.incrementAndGet();
    }

    public void finishWriting() {
        counter.incrementAndGet();
        lock.unlock();
    }

    /**
     * Writing threads are not allowed to interfer with this reading process. But reading threads
     * can read while exclusiveReading is hold.
     * Use this for time consuming read operations that cannot easily retried
     */
    public void startExclusiveReading() {
        lock.lock();
    }

    public void finishExclusiveReading() {
        lock.unlock();
    }

    public long startReading() {
        while (true) {
            final long count = counter.get();
            if (count % 2 == 0) return count;
        }
    }

    public boolean canFinishReading(long value) {
        return counter.get()==value;
    }

    public boolean needToRetryReading(long value) {
        return !canFinishReading(value);
    }

}
