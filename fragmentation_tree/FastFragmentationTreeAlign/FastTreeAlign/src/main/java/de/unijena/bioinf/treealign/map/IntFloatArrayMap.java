
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

package de.unijena.bioinf.treealign.map;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * implements a map int to float as array.
 * The memory usage for values depends on the maximum number of possible entries.
 */
public class IntFloatArrayMap implements IntFloatMap {

    private final static float DEFAULT_VALUE = 0;
    private final int maxSize;
    private int size;
    private float[] values;
    private int[] keys;

    public IntFloatArrayMap(final int maxSize) {
        if (maxSize <= 0)
            throw new IllegalArgumentException("maxSize have to be greater than 0, but " +
                    maxSize +  " is given");
        this.maxSize = maxSize;
        this.size = 0;
        this.values = null;
        this.keys = null;
    }

    private void allocate() {
         if (values == null) {
             values = new float[maxSize];
             keys  = new int[Math.min(maxSize, 32)];
             Arrays.fill(values, Float.NaN);
         }
    }
    
    private void addKey(int key) {
        if (size() >= keys.length) {
            keys = Arrays.copyOf(keys, Math.min(maxSize, keys.length*2));
        }
        keys[size++] = key;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return (values == null ? 0 : values.length);
    }

    public int collisions() {
        return 0;
    }

    public int collisionKeys() {
        return 0;
    }

    public int reallocations() {
        return 0;
    }

    public boolean isHash() {
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public float get(int A) {
        if (values == null) {
            return DEFAULT_VALUE;
        } else {
            final float value = values[A];
            return (Float.isNaN(value)) ? DEFAULT_VALUE : value;
        }
    }

    public void put(int A, float value) {
        if (A == 0) throw new IllegalArgumentException("0 can't be a key of this map");
        if (Float.isNaN(value)) throw new IllegalArgumentException("NaN can't be a value of this map");
        allocate();
        if (!Float.isNaN(values[A])) {
            addKey(A);
        }
        values[A] = value;
    }

    public void putIfGreater(int A, float value) {
    	if (Float.isNaN(value)) throw new IllegalArgumentException("NaN can't be a value of this map");
        if (A == 0) throw new IllegalArgumentException("0 can't be a key of this map");
        if (value < DEFAULT_VALUE) return;
        allocate();
        final float val = values[A];
        if (Float.isNaN(val)) {
            addKey(A);
            values[A] = value;
        } else if (val < value) {
            values[A] = value;
        }
    }

    private class KeyValueIterator implements IntFloatIterator {
        private int index;
        private KeyValueIterator() {
            index = -1;
        }

        public float getValue() {
            if (index < 0 || index >= keys.length) throw new NoSuchElementException();
            return values[keys[index]];
        }

        public void next() {
            if (!hasNext()) throw new NoSuchElementException();
            ++index;
        }

        public int getKey() {
            if (index < 0 || index >= keys.length) throw new NoSuchElementException();
            return keys[index];
        }

        public boolean hasNext() {
            return index < (size-1);
        }
    }


    public IntFloatIterator entries() {
        return (values == null) ? IntFloatIterator.Empty : new KeyValueIterator();
    }

}
