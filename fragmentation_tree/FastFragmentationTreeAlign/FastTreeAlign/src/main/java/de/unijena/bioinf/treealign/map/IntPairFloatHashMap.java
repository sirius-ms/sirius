
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

public class IntPairFloatHashMap implements IntPairFloatMap {

    private static final long serialVersionUID = 1142899365261456647L;

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 8;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<31.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    static final int MINIMUM_CAPACITY = 4;

    /**
     * The load factor used when none specified in constructor.
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private int[] As;
    private int[] Bs;
    private float[] values;

    private final int capacity;

    private byte resizes;

    /**
     * The number of key-value mappings contained in this map.
     */
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     */
    final float loadFactor;

    /**
     * Constructs an empty HashMap with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public IntPairFloatHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (initialCapacity < MINIMUM_CAPACITY)
            initialCapacity = MINIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);

        // Find a power of 2 >= initialCapacity
        this.resizes = 0;
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        this.capacity = capacity;
    }

    public IntPairFloatHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public IntPairFloatHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        this.capacity = DEFAULT_INITIAL_CAPACITY;
    }

    // internal utilities

    static int hash(int A, int B, int tries) {
        int h = A * (B<<1);
        h ^= (h >> 7);
        return (int) (h+0.5*tries+0.5*tries*tries);

    }

    static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    public int capacity() {
        return (values == null) ? 0 : values.length;
    }

    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return true if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size == 0;
    }

    public float get(int A, int B) {
        if (values == null) return DEFAULT_VALUE;
        if (A == 0 && B == 0){
            return 0;       // TODO: CHange
            //throw new IllegalArgumentException("0 is never a key of this map");
        }
        int index = indexFor(hash(A, B, 0), values.length);
        int l = As[index];
        int r = Bs[index];
        for (int tries = 1; (l != 0 || r != 0) && (A != l || B != r) && tries < values.length; ++tries){
            index = indexFor(hash(A, B, tries), values.length);
            l = As[index];
            r = Bs[index];
        }
        if (l==A && r == B){
            return values[index];
        }
        return DEFAULT_VALUE;
    }

    public void put(int A, int B, float value) {
        allocate();
        if (A == 0 && B == 0){
            throw new IllegalArgumentException("0 is never a key of this map");
        }
        int index = indexFor(hash(A, B, 0), values.length);
        int l = As[index];
        int r = Bs[index];
        for (int tries = 1; (l != 0 || r != 0) && (A != l || B != r) && tries < values.length; ++tries){
            index = indexFor(hash(A, B, tries), values.length);
            l = As[index];
            r = Bs[index];
        }
        if (l==A && r == B){
            values[index] = value;
        } else if (l == 0 && r == 0){
            values[index] = value;
            As[index] = A;
            Bs[index] = B;
            ++size;
            if (size >= threshold){
                resize(2 * values.length);
            }
        } else {
            throw new RuntimeException("Map is full");
        }
    }

    public IntPairFloatMap.ReturnType putIfGreater(int A, int B, float value) {
        if (value < DEFAULT_VALUE) return ReturnType.LOWER;
        allocate();
        if (A == 0 && B == 0){
            throw new IllegalArgumentException("0 is never a key of this map");
        }
        int index = indexFor(hash(A, B, 0), values.length);
        int l = As[index];
        int r = Bs[index];
        for (int tries = 1; (l != 0 || r != 0) && (A != l || B != r) && tries < values.length; ++tries){
            index = indexFor(hash(A, B, tries), values.length);
            l = As[index];
            r = Bs[index];
        }
        if (l==A && r == B){
            if (value > values[index]) {
                values[index] = value;
                return ReturnType.GREATER;
            } else {
                return ReturnType.LOWER;
            }
        } else if (l == 0 && r == 0) {
            values[index] = value;
            As[index] = A;
            Bs[index] = B;
            ++size;
            if (size >= threshold){
                resize(2 * values.length);
            }
            return ReturnType.NOT_EXIST;
        } else {
            throw new RuntimeException("Map is full");
        }
    }

    // TODO: REMOVE
    public float averageOpsPerAccess() {
        if (size() < 10) return 0;
        final IntPairFloatIterator iter = entries();
        int ops = 0;
        while (iter.hasNext()) {
            iter.next();
            final int A = iter.getLeft();
            final int B = iter.getRight();
            int t = 0;
            {
                ++ops;
                int index = indexFor(hash(A, B, 0), values.length);
                int l = As[index];
                int r = Bs[index];
                for (int tries = 1; (l != 0 || r != 0) && (A != l || B != r) && tries < values.length; ++tries){
                    index = indexFor(hash(A, B, tries), values.length);
                    l = As[index];
                    r = Bs[index];
                    ++ops;
                }
            }
        }
        return ((float)ops)/((float)size());
    }

    private void allocate() {
        if (values == null) {
            try {
                this.As = new int[capacity];
                this.Bs = new int[capacity];
                this.values = new float[capacity];
                Arrays.fill(values, Float.NaN);
            } catch (OutOfMemoryError err) {
                System.err.println(capacity);
                throw new RuntimeException(err);
            }
        }
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of entries in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    void resize(int newCapacity) {
        ++resizes;
        int[] oldAs = As;
        int[] oldBs = Bs;
        float[] oldValues = values;
        int oldCapacity = oldAs.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        As = new int[newCapacity];
        Bs = new int[newCapacity];
        values = new float[newCapacity];
        Arrays.fill(values, Float.NaN);
        size = 0; //size will be set by put
        threshold = (int)(newCapacity * loadFactor);
        for (int i = 0; i<oldAs.length; ++i){
            if (oldAs[i] != 0){
                put(oldAs[i], oldBs[i], oldValues[i]);
            }
        }
    }

    private int collisionInspection(boolean countMultiple) {
        if (capacity() == 0) return 0;
        int cs = 0;
        int cKeys = 0;
        if (capacity() == 0) return 0;
        for (int i=0; i < As.length; ++i) {
            if (As[i] == 0 && Bs[i] == 0) continue;
            final int A = As[i];
            final int B = Bs[i];
            int t = 0;
            while (indexFor(hash(A, B, t), As.length) != i)
                ++t;
            if (t > 0) ++cKeys;
            cs += t;
        }
        return countMultiple ? cs : cKeys;
    }

    public int collisionKeys() {
        return collisionInspection(false);
    }

    public int reallocations() {
        return resizes;
    }

    public boolean isHash() {
        return true;
    }

    public int collisions() {
        return collisionInspection(true);
    }

    public class KeyIterator implements IntPairFloatIterator {
        private int index;
        private int A=0;
        private int B=0;
        private float value = Float.NaN;
        private boolean initialized = false;

        public KeyIterator(){
            index=0;
            findNext();
        }

        private void findNext() {
            while (index < values.length && (As[index] == 0 && Bs[index] == 0)){
                ++index;
            }
        }

        public void next(){
            if (index >= As.length){
                throw new NoSuchElementException();
            }
            this.A = As[index];
            this.B = Bs[index];
            this.value = values[index];
            initialized = true;
            ++index;
            findNext();
        }

        public boolean hasNext(){
            return index < As.length;
        }

        public int getLeft() {
            if (!initialized) throw new NoSuchElementException();
            return A;
        }

        public int getRight() {
            if (!initialized) throw new NoSuchElementException();
            return B;
        }

        public float getValue() {
            if (!initialized) throw new NoSuchElementException();
            return value;
        }

    }

    public IntPairFloatIterator entries() {
        if (values == null)return IntPairFloatIterator.Empty;
        return new KeyIterator();
    }

}
