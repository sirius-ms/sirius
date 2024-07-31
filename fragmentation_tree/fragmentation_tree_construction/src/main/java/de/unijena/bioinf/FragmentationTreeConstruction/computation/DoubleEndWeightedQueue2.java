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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import gnu.trove.procedure.TObjectProcedure;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * A queue that only keeps the n entries with largest weight
 */
class DoubleEndWeightedQueue2<T> implements Iterable<T> {

    protected TreeMap<Double, SortedSet<T>> backingQueue;
    protected int capacity;
    protected int size;
    protected double lowerbound;
    @Setter
    @Getter
    protected TObjectProcedure<T> callback;

    private Function<Double, SortedSet<T>> sortedSetSupplier;
    public DoubleEndWeightedQueue2(int capacity, Comparator<T> comp) {
        this.sortedSetSupplier = k -> new TreeSet<>(comp);
        this.backingQueue = new TreeMap<>(Comparator.comparingDouble(o -> o));
        lowerbound = Double.NEGATIVE_INFINITY;
        size=0;
        this.capacity = capacity;
    }

    public void replace(T value, double score) {
        if (callback!=null) callback.execute(value);
        if (backingQueue.containsKey(score))
            backingQueue.get(score).remove(value);
        backingQueue.computeIfAbsent(score, sortedSetSupplier).add(value);
    }

    public double getWeightLowerbound() {
        return lowerbound;
    }

    public boolean add(final T tree, final double score) {
        if (score > lowerbound) {
            if (backingQueue.computeIfAbsent(score, sortedSetSupplier).add(tree)) {
                ++size;
                while (size > capacity) {

                    Map.Entry<Double, SortedSet<T>> entry = backingQueue.firstEntry();
                    final int entrySize = entry.getValue().size();
                    if ((size - entrySize) >= capacity ) {
                        Map.Entry<Double, SortedSet<T>> e =  backingQueue.pollFirstEntry();
                        if (callback!=null)
                            for (T t : e.getValue()) callback.execute(t);
                        size -= entrySize;
                    } else {
                        break;
                    }
                }
                if (size >= capacity) {
                    lowerbound = backingQueue.firstKey();
                } else {
                    lowerbound = Double.NEGATIVE_INFINITY;
                }
                return true;
            } else return false;
        } else return false;
    }

    public List<T> getTrees() {
        final ArrayList<T> list = new ArrayList<>(capacity);
        for (T tree : this) list.add(tree);
        return list;
    }

    public void clear() {
        size=0;
        lowerbound = Double.NEGATIVE_INFINITY;
        if (callback!=null)
            backingQueue.values().stream().flatMap(SortedSet::stream).forEach(t -> callback.execute(t));
        backingQueue.clear();
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        final Iterator<SortedSet<T>> iter = backingQueue.descendingMap().values().iterator();
        return new Iterator<T>() {

            private Iterator<T> innerIterator = null;

            @Override
            public boolean hasNext() {
                if (innerIterator==null || !innerIterator.hasNext()) {
                    if (iter.hasNext()) innerIterator = iter.next().iterator();
                    else return false;
                }
                return true;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return innerIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
