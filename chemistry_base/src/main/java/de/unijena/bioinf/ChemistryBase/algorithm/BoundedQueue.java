
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

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.IntFunction;

/**
 * A fixed size queue of double values. Inserting a new value into a full queue will lead to a removal of the smallest element
 */
public final class BoundedQueue<T> implements Iterable<T> {

    private final T[] values;
    private int length;
    private final Comparator<T> comparator;
    private Callback<T> callbackForRemoval;

    public BoundedQueue(int size, IntFunction<T[]> generator, Comparator<T> comparator) {
        this.values = generator.apply(size);
        this.length = 0;
        this.comparator = comparator;
        this.callbackForRemoval = null;
    }

    public Callback<T> getCallbackForRemoval() {
        return callbackForRemoval;
    }

    public void setCallbackForRemoval(Callback<T> callbackForRemoval) {
        this.callbackForRemoval = callbackForRemoval;
    }

    public int length() {
        return length;
    }

    public T min() {
        return (T)values[0];
    }

    public T max() {
        return (T)values[length-1];
    }

    public boolean add(T value) {
        if (length < values.length) {
            values[length++] = value;
            Arrays.sort(values, 0, length, comparator);
            return true;
        }
        if (comparator.compare(value, values[0]) < 0) return false;
        if (callbackForRemoval!=null) callbackForRemoval.call(values[0]);
        final int index = (length <= 5) ? linearSearch(value) : binarySearch(value);
        if (index > 0) {
            if (index > 1) System.arraycopy(values, 1, values, 0, index-1);
            values[index-1] = value;
            return true;
        } else return false;
    }

    private int binarySearch(T value) {
        final int index = Arrays.binarySearch(values, 0, length, value, comparator);
        if (index >= 0) return index;
        else return (-index-1);
    }

    private int linearSearch(T value) {
        for (int i=0; i < length; ++i) {
            if (comparator.compare(values[i],value)>0) {
                return i;
            }
        }
        return length;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        if (length >= values.length)
            return Arrays.stream(values).iterator();
        else return Arrays.stream(values).limit(length).iterator();
    }

    public T[] toArray() {
        if (length>=values.length)
            return values.clone();
        else
            return Arrays.copyOf(values, length);
    }

    public static interface Callback<T> {
        public void call(T value);
    }
}
