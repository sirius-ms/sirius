
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
 * Implementation of a map (int, int) to float.
 * Provides fast get and put operations.
 * Remark that iterating this map is VERY slow, because it doesn't save the keys. So
 * don't use an iterator of this map in time critical situations
 * (note: in the dp tree align algorithm this map is only iterated in the backtracking, which is
 * not time critical)
 */
public class IntPairFloatArrayMap implements IntPairFloatMap {

    private final int leftSize;
    private final int rightSize;
    private int size;
    private float[] values;

    public IntPairFloatArrayMap(final int leftSize, final int rightSize) {
        if (leftSize <= 0 || rightSize <= 0)
            throw new IllegalArgumentException("leftSize and rightSize have to be greater than 0, but " +
                    leftSize + " and " + rightSize + " given");
        this.leftSize = leftSize;
        this.rightSize = rightSize;
        this.size = 0;
        this.values = null;
    }

    private void allocate() {
         if (values == null) {
             final long size = (long)leftSize*(long)rightSize;
             if (size > Integer.MAX_VALUE) {
                 throw new OutOfMemoryError();
             } else {
                 values = new float[(int)size];
             }
             Arrays.fill(values, Float.NaN);
         }
    }

    private void checkRange(int A, int B) {
        if (A >= leftSize)
            throw new IndexOutOfBoundsException("left index " + A + " is out of bound " + leftSize);
        if (B >= rightSize)
            throw new IndexOutOfBoundsException("right index " + B + " is out of bound " + rightSize);
        assert values == null || A+B*leftSize < values.length;
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

    public float get(int A, int B) {
        checkRange(A, B);
        if (values == null) {
            return DEFAULT_VALUE;
        } else {
            final float value = values[A + B*leftSize];
            return (Float.isNaN(value)) ? DEFAULT_VALUE : value;
        }
    }

    public void put(int A, int B, float value) {
        checkRange(A, B);
        allocate();
        if (Float.isNaN(value)) throw new IllegalArgumentException("NaN values cannot be stored in this map");
        final int index = A + B*leftSize;
        if (Float.isNaN(values[index])) {
            ++size;
        }
        values[index] = value;
    }

    public ReturnType putIfGreater(int A, int B, float value) {
        checkRange(A, B);
        if (value < DEFAULT_VALUE) return ReturnType.LOWER;
        allocate();
        if (Float.isNaN(value)) throw new IllegalArgumentException("NaN values cannot be stored in this map");
        final int index = A  + B*leftSize;
        final float val = values[index];
        if (Float.isNaN(val)) {
            ++size;
            values[index] = value;
            return ReturnType.NOT_EXIST;
        } else if (val < value) {
            values[index] = value;
            return ReturnType.GREATER;
        } else {
            return ReturnType.LOWER;
        }
    }

    public IntPairFloatIterator entries() {
        if (values == null || values.length == 0) return IntPairFloatIterator.Empty;
        return new KeyValueIterator();
    }

    private class KeyValueIterator  implements IntPairFloatIterator {
        private int index;
        private int prevIndex;
        private KeyValueIterator() {
            index = -1;
            prevIndex = -1;
            findNext();
        }
        protected void findNext() {
            do {
                ++index;
            } while (index < values.length && Float.isNaN(values[index]));
        }

        protected int getA() {
            return prevIndex % leftSize;
        }

        protected int getB() {
            return prevIndex / leftSize;
        }

        public float getValue() {
            return values[prevIndex];
        }

        public boolean hasNext() {
            return index < values.length;
        }

        private int A = 0;
        private int B = 0;

        public int getLeft() {
            return A;
        }

        public int getRight() {
            return B;
        }

        public void next() {
            if (!hasNext()) throw new NoSuchElementException();
            prevIndex = index;
            A = getA();
            B = getB();
            findNext();
        }
    }
}
