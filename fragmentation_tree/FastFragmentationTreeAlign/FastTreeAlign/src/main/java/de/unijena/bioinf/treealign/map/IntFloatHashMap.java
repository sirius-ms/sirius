
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

public class IntFloatHashMap implements IntFloatMap {

    private final static float DEFAULT_LOAD_FACTOR = 0.75f;
    private int[] keys;
    private float[] values;
    private int capacity;
    private int size;
    private int threshold;


    private byte resizes;

    public IntFloatHashMap(int size) {
        if (size < 0 || size > (1<<30)) {
            throw new IllegalArgumentException("size " + size + " is out of bound <0, 2^30>");
        }
        int pot = 1;
        while (pot < size) pot <<= 1;
        this.capacity = pot;
        this.size = 0;
        this.threshold = (int)(capacity * DEFAULT_LOAD_FACTOR);
        this.resizes = 0;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return (values == null ? 0 : values.length);
    }

    // TODO: REMOVE
    public float averageOpsPerAccess() {
        if (size() < 10) return 0;
        final IntFloatIterator iter = entries();
        int ops = 0;
        while (iter.hasNext()) {
            iter.next();
            final int A = iter.getKey();
            int t = 0;
            {
                ++ops;
                int index = indexFor(hash(A, 0), keys.length);
                int k = keys[index];
                for (int tries = 1; k != 0 && k != A && tries < keys.length; ++tries){
                    index = indexFor(hash(A, tries), keys.length);
                    k = keys[index];
                    ++ops;
                }
            }
        }
        return ((float)ops)/((float)size());
    }

    private int collisionInspection(boolean countMultiple) {
        int cs = 0;
        int cKeys = 0;
        if (capacity() == 0) return 0;
        for (int i=0; i < keys.length; ++i) {
            if (keys[i] == 0) continue;
            final int key = keys[i];
            int t = 0;
            while (indexFor(hash(key, t), keys.length) != i)
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

    public boolean isEmpty() {
        return size == 0;
    }

    public float get(int A) {
        if (values != null) {
            if (A == 0){
                throw new IllegalArgumentException("0 is never a key of this map");
            }
            int index = indexFor(hash(A, 0), keys.length);;
            int k = keys[index];
            for (int tries = 1; k != 0 && k != A && tries < keys.length; ++tries){
                index = indexFor(hash(A, tries), keys.length);
                k = keys[index];
            }
            if (k==A){
                return values[index];
            }
        }
        return DEFAULT_VALUE;
    }

    public void put(int key, float value) {
        if (key <= 0){
            throw new IllegalArgumentException("Non positive keys like " + key + " may not be entered into this map");
        }
        if (Float.isNaN(value)){
            throw new IllegalArgumentException("NaN may not be entered into this map");
        }
        allocate();

        int index = indexFor(hash(key, 0), keys.length);
        int k = keys[index];
        for (int tries = 1; k != 0 && k != key && tries < keys.length; ++tries){
            index = indexFor(hash(key, tries), keys.length);
            k = keys[index];
        }
        if (k != 0 && k!= key){
            throw new RuntimeException("Map is full");
        }
        values[index] = value;
        if (k == 0) {
           keys[index] = key;
            ++size;
            if (size >= threshold){
                resize(2 * keys.length);
            }
        }
    }

    public void putIfGreater(int key, float value) {
        if (key == 0){
            throw new IllegalArgumentException("0 may not be entered into this map");
        }
        if (Float.isNaN(value)){
            throw new IllegalArgumentException("NaN may not be entered into this map");
        }
        if (value < DEFAULT_VALUE) return;
        allocate();

        int index = indexFor(hash(key, 0), keys.length);
        int k = keys[index];
        for (int tries = 1; k != 0 && k != key && tries < keys.length; ++tries){
            index = indexFor(hash(key, tries), keys.length);
            k = keys[index];
        }
        if (k != 0 && k!= key){
            throw new RuntimeException("Map is full");
        }

        final float oldValue = values[index];
        if (k == 0) {
            keys[index] = key;
            values[index] = value;
            ++size;
            if (size >= threshold){
                resize(2 * keys.length);
            }
        } else if (oldValue < value) {
            values[index] = value;
        }
    }

    private void resize(int newCapacity) {
        ++resizes;
        int[] oldKeys = keys;
        float[] oldValues = values;
        int oldCapacity = oldKeys.length;
        if (oldCapacity == 2<<30) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        keys = new int[newCapacity];
        values = new float[newCapacity];
        Arrays.fill(values, Float.NaN);
        size = 0; //size will be set by put
        threshold = (int)(newCapacity * DEFAULT_LOAD_FACTOR);
        for (int i = 0; i<oldKeys.length; ++i){
            if (oldKeys[i] != 0){
                put(oldKeys[i], oldValues[i]);
            }
        }
    }

    public class KeyValueIterator implements IntFloatIterator {
        private int index;
        private int prevIndex;

        public KeyValueIterator(){
            prevIndex = -1;
            index = -1;
            findNext();
        }

        public void next() {
            prevIndex = index;
            findNext();
        }

        private void findNext(){
            if (index >= keys.length){
                throw new NoSuchElementException();
            }
            do {
                ++index;
            } while (index < keys.length && keys[index] == 0);
        }

        public int getKey() {
            if (prevIndex < 0 || prevIndex >= keys.length)
                throw new NoSuchElementException();
            return keys[prevIndex];
        }

        public float getValue() {
            if (prevIndex < 0 || prevIndex >= keys.length)
                throw new NoSuchElementException();
            return values[prevIndex];
        }

        public boolean hasNext(){
            return index < keys.length;
        }

    }

    public IntFloatIterator entries() {
        return (values == null) ? IntFloatIterator.Empty : new KeyValueIterator();
    }

    private static void checkSize(int size) {
        if (size > (1<<30)) {
            throw new IllegalArgumentException("size " + size + " is out of bound <0, 2^30>");
        }
    }

    private static int hash(int key, int tries) {
        return (int) (key+0.5*tries+0.5*tries*tries);
    }

    private static int indexFor(int h, int length) {
        return h & (length-1);
    }

    private void allocate() {
        if (values == null) {
            values = new float[capacity];
            keys = new int[capacity];
        }
    }
}
