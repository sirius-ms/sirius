/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.storage.db.nosql.Database;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public abstract class SiriusProjectDatabaseImpl<Storage extends Database<?>> implements SiriusProjectDocumentDatabase<Storage>, Closeable, AutoCloseable {

    protected final Storage storage;
    private final LongSet alignedFeatureComputeStates = new LongOpenHashSet();
    private final Set<Consumer<ComputeStateEvent>> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ReentrantReadWriteLock alignedFeatureComputeStatesLock = new ReentrantReadWriteLock();


    public SiriusProjectDatabaseImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }

    @Override
    public void close() throws IOException {
        storage.close();
    }

    @Override
    public void setFeaturesComputing(boolean computing, Collection<Long> alignedFeatureId) {
        alignedFeatureComputeStatesLock.writeLock().lock();

        if (computing) {
            if (alignedFeatureComputeStates.addAll(alignedFeatureId))
                fireComputeChangeEvent(true, new LongOpenHashSet(alignedFeatureId));
        } else {
            if (alignedFeatureComputeStates.removeAll(alignedFeatureId))
                fireComputeChangeEvent(false, new LongOpenHashSet(alignedFeatureId));
        }
    }

    @Override
    public void setFeatureComputing(boolean computing, long alignedFeatureId) {
        alignedFeatureComputeStatesLock.writeLock().lock();
        try {
            if (computing) {
                if (alignedFeatureComputeStates.add(alignedFeatureId))
                    fireComputeChangeEvent(true, LongSet.of(alignedFeatureId));
            } else {
                if (alignedFeatureComputeStates.remove(alignedFeatureId))
                    fireComputeChangeEvent(false, LongSet.of(alignedFeatureId));
            }
        } finally {
            alignedFeatureComputeStatesLock.writeLock().unlock();
        }

    }

    @Override
    public boolean isFeatureComputing(long alignedFeatureId) {
        alignedFeatureComputeStatesLock.readLock().lock();
        try {
            return alignedFeatureComputeStates.contains(alignedFeatureId);
        } finally {
            alignedFeatureComputeStatesLock.readLock().unlock();
        }
    }

    protected void fireComputeChangeEvent(boolean computeState, LongSet alignedFeatureIds) {
        final ComputeStateEvent evt = ComputeStateEvent.builder()
                .newComputeState(computeState)
                .affectedFeatureIds(alignedFeatureIds).build();
        listeners.forEach(c -> c.accept(evt));
    }

    @Override
    public boolean onCompute(Consumer<ComputeStateEvent> listener) {
        return this.listeners.add(listener);
    }

    @Override
    public boolean unsubscribe(Consumer<ComputeStateEvent> listener) {
        return listeners.remove(listener);
    }
}
