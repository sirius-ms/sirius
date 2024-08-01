
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

package de.unijena.bioinf.treealign.multijoin;

import de.unijena.bioinf.treealign.Tree;
import de.unijena.bioinf.treealign.map.*;

import java.util.List;

/**
 * The dynamic programming table S(A,B) for a given pair of vertices (u, v).
 * Instead of a nonsparse Table, this table should be more efficient if it is filled with zero
 * or few entries.
 */
class HashTable<T> {

    /**
     * percent of table entries which will be allocated for new nonempty hash tables.
     * A value of 0.1 means: We expect that nonenmpty tables are filled in average 10%
     * Nevertheless, a hash table is allocated with at most MAX_INITIAL_ENTRY_SLOTS entries
     */
    public final static float INITIAL_FILL_SIZE_FOR_SMALL = 0.2f;
    public final static float INITIAL_FILL_SIZE_FOR_HUGE = 0.05f;
    /**
     * Maximum initial size of hashtables.
     */
    final static int MAX_INITIAL_ENTRY_SLOTS = 1024;

    /**
     * implementations of the hashtable. Can be changed at runtime
     */
    static enum Mode {USE_ARRAY, USE_HASH, USE_HASH_FOR_HUGE_DEGREE};


    private final static Mode MODE = Mode.USE_HASH_FOR_HUGE_DEGREE;

    private final IntPairFloatMap data;
    private final IntFloatMap maxLeft;
    private final IntFloatMap maxRight;
    private final JoinTable[][] join;
    private float score;

    HashTable() {
        this.data = null;
        this.join = null;
        this.maxLeft = null;
        this.maxRight = null;
        this.score = 0f;
    }

    HashTable(List<Tree<T>> basicSetA, List<Tree<T>> basicSetB, short L, short R) {
        final int leftSize = 1 << basicSetA.size();
        final int rightSize = 1 << basicSetB.size();
        this.data = allocateIntPairFloatMap(leftSize, rightSize);
        this.join = new JoinTable[L+1][R+1];
        for (int l=0; l <= L; ++l) {
            for (int r=0; r <= R; ++r) {
                if (l != 0 || r != 0)
                    this.join[l][r] = new JoinTable(leftSize, rightSize, l, r);
            }
        }
        this.maxLeft = allocateIntFloatMap(rightSize);
        this.maxRight = allocateIntFloatMap(leftSize);
        this.score = 0.0f;
    }

    private static IntFloatMap allocateIntFloatMap(int size) {
        switch (MODE) {
            case USE_ARRAY:
                return new IntFloatArrayMap(size);
            case USE_HASH:
                return new IntFloatHashMap(trimSizeForHashMap(size));
            case USE_HASH_FOR_HUGE_DEGREE:
                return (isHuge(size)) ? new IntFloatHashMap(trimSizeForHashMap(size))
                        : new IntFloatArrayMap(size) ;
            default: throw new RuntimeException("Illegal value for MODE: " + String.valueOf(MODE));
        }
    }

    private static boolean isHuge(long size) {
        return size >= 262144;
    }

    private static int trimSizeForHashMap(long size) {
        long l = 0;
        if (isHuge(size)) {
            l = (long)(size * INITIAL_FILL_SIZE_FOR_HUGE);
        } else {
            l = (long)(size * INITIAL_FILL_SIZE_FOR_SMALL) + 2;
        }
        l = Math.min(size, Math.min(l, MAX_INITIAL_ENTRY_SLOTS));
        assert l < Integer.MAX_VALUE;
        return (int)l;
    }

    private static IntPairFloatMap allocateIntPairFloatMap(int leftSize, int rightSize) {
        final long size = (long)leftSize * (long)rightSize;
        switch (MODE) {
            case USE_ARRAY:
                return new IntPairFloatArrayMap(leftSize, rightSize);
            case USE_HASH:
                return new IntPairFloatHashMap(trimSizeForHashMap(size));
            case USE_HASH_FOR_HUGE_DEGREE:
                return (isHuge(size)) ?  new IntPairFloatHashMap(trimSizeForHashMap(size))
                        : new IntPairFloatArrayMap(leftSize, rightSize);
            default: throw new RuntimeException("Illegal value for MODE: " + String.valueOf(MODE));
        }
    }

    float getScore() {
        return score;
    }

    void setScore(float score) {
        this.score = score;
    }

    void setScoreIfGreater(float score) {
        this.score = Math.max(score, this.score);
    }

    float get(int A, int B) {
        if (data == null) return 0;
        return data.get(A, B);
    }

    void set(int A, int B, float value) {
        data.put(A, B, value);
        assert data.get(A, B) > 0;
    }

    IntPairFloatMap.ReturnType putIfGreater(int A, int B, float value) {
        return data.putIfGreater(A, B, value);
    }

    float getJoin(short l, short r, int A, int B) {
        if (join == null) return 0;
        return join[l][r].data.get(A, B);
    }

    void setJoin(short l, short r, int A, int B, float value) {
        join[l][r].data.put(A, B, value);
    }

    IntPairFloatMap.ReturnType putJoinIfGreater(short l, short r, int A, int B, float value) {
        return join[l][r].data.putIfGreater(A, B, value);
    }

    float getMaxLeft(int B) {
        if (maxLeft == null) return 0;
        return maxLeft.get(B);
    }
    float getMaxRight(int A) {
        if (maxRight == null) return 0;
        return maxRight.get(A);
    }
    float getMaxJoinLeft(short l, short r,  int B) {
        if (join == null) return 0;
        return join[l][r].maxLeft.get(B);
    }
    float getMaxJoinRight(short l, short r, int A) {
        if (join == null) return 0;
        return join[l][r].maxRight.get(A);
    }

    IntFloatIterator eachInMaxLeft() {
        if (maxLeft == null) return IntFloatIterator.Empty;
        return maxLeft.entries();
    }
    IntFloatIterator eachInMaxRight() {
        if (maxRight == null) return IntFloatIterator.Empty;
        return maxRight.entries();
    }
    IntFloatIterator eachInMaxJoinLeft(short l, short r) {
        if (join == null) return IntFloatIterator.Empty;
        return join[l][r].maxLeft.entries();
    }
    IntFloatIterator eachInMaxJoinRight(short l, short r) {
        if (join == null) return IntFloatIterator.Empty;
        return join[l][r].maxRight.entries();
    }
    IntPairFloatIterator each() {
        if (data == null) return IntPairFloatIterator.Empty;
        return data.entries();
    }
    IntPairFloatIterator eachInJoin(short l, short r) {
        if (join == null) return IntPairFloatIterator.Empty;
        return join[l][r].data.entries();
    }


    void putMaxLeftIfGreater(int B, float value) {
        maxLeft.putIfGreater(B, value);
    }

    void putMaxRightIfGreater(int A, float value) {
        maxRight.putIfGreater(A, value);
    }

    void putMaxJoinLeftIfGreater(short l, short r, int B, float value) {
        join[l][r].maxLeft.putIfGreater(B, value);
    }

    void putMaxJoinRightIfGreater(short l, short r, int A, float value) {
        join[l][r].maxRight.putIfGreater(A, value);
    }

    private final static class JoinTable {
        private final IntFloatMap maxLeft, maxRight;
        private final IntPairFloatMap data;
        private JoinTable(int leftSize, int rightSize, int l, int r)  {
            this.data = allocateIntPairFloatMap(leftSize, rightSize);
            this.maxLeft = (l == 0) ? null : allocateIntFloatMap(rightSize);
            this.maxRight = (r == 0) ? null : allocateIntFloatMap(leftSize);
        }
    }

    /*
    public void inspectMaps(MapInspector inspector, Class<?> filterClass) {
        for (MapInspectable spec : Arrays.asList(data, joinDataLeft, joinDataRight, maxJoinLeft, maxJoinRight, maxLeft, maxRight)) {
            if (filterClass.isInstance(spec)) {
                inspector.inspect(spec);
            }
        }
    }

    public void inspectMaps(MapInspector inspector) {
        for (MapInspectable spec : allMaps()) {
            inspector.inspect(spec);
        }
    }

    public void inspectHashMaps(MapInspector inspector) {
        for (MapInspectable spec : allMaps()) {
            if (spec instanceof IntFloatHashMapTest || spec instanceof IntPairFloatHashMap)
                inspector.inspect(spec);
        }
    }
    public void inspectArrayMaps(MapInspector inspector) {
        for (MapInspectable spec : allMaps()) {
            if (spec instanceof IntFloatArrayMap || spec instanceof IntPairFloatArrayMap)
                inspector.inspect(spec);
        }
    }

    private MapInspectable[] allMaps() {
        return new MapInspectable[]{data, joinDataLeft, joinDataRight, maxJoinLeft, maxJoinRight, maxLeft, maxRight};
    }

    private static class MapInfo implements MapInspectable {

        private final int reallocates, size, capacity, collisions, collisionKeys;
        private final boolean isHash;

        private MapInfo(MapInspectable inspectable) {
            this.size = inspectable.size();
            this.capacity = inspectable.capacity();
            this.collisions = inspectable.collisions();
            this.collisionKeys = inspectable.collisionKeys();
            this.isHash = inspectable.isHash();
            this.reallocates = inspectable.reallocations();
        }

        public int size() {
            return size;
        }

        public int capacity() {
            return capacity;
        }

        public int collisions() {
            return collisions;
        }

        public int collisionKeys() {
            return collisionKeys;
        }

        public int reallocations() {
            return reallocates;
        }

        public boolean isHash() {
            return isHash;
        }
    }
    */


}
