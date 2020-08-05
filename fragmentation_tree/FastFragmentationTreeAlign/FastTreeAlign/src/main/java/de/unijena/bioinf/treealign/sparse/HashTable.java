
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

package de.unijena.bioinf.treealign.sparse;

import de.unijena.bioinf.treealign.Tree;
import de.unijena.bioinf.treealign.map.*;

import java.util.List;

/**
 * A table maps a pair (A, B) with A and B are sets to a scoring
 */
class HashTable<T> {

    public final static double INITIAL_FILL_SIZE_FOR_SMALL = 0.2f;
    public final static double INITIAL_FILL_SIZE_FOR_HUGE = 0.05f;
    public final static int MAX_INITIAL_ENTRY_SLOTS = 1024;

    public static enum Mode {USE_ARRAY, USE_HASH, USE_HASH_FOR_HUGE_DEGREE};

    public final static Mode MODE = Mode.USE_HASH_FOR_HUGE_DEGREE;

    private final IntPairFloatMap data;
    private final IntPairFloatMap joinDataLeft;
    private final IntPairFloatMap joinDataRight;
    private final IntFloatMap maxLeft;
    private final IntFloatMap maxRight;
    private final IntFloatMap maxJoinLeft;
    private final IntFloatMap maxJoinRight;
    private float score;

    public HashTable(List<Tree<T>> basicSetA, List<Tree<T>> basicSetB, boolean useJoins) {
        final int leftSize = 1 << basicSetA.size();
        final int rightSize = 1 << basicSetB.size();
        this.data = allocateIntPairFloatMap(leftSize, rightSize);
        if (useJoins) {
            this.joinDataLeft = allocateIntPairFloatMap(leftSize, rightSize);
            this.joinDataRight = allocateIntPairFloatMap(leftSize, rightSize);
            this.maxJoinLeft = allocateIntFloatMap(rightSize);
            this.maxJoinRight = allocateIntFloatMap(leftSize);
        } else {
            this.joinDataLeft = null;
            this.joinDataRight = null;
            this.maxJoinLeft = null;
            this.maxJoinRight = null;
        }
        this.maxLeft = allocateIntFloatMap(rightSize);
        this.maxRight = allocateIntFloatMap(leftSize);
        this.score = 0.0f;
    }

    private IntFloatMap allocateIntFloatMap(int size) {
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

    public boolean isHuge(long size) {
        return size >= 262144;
    }

    private int trimSizeForHashMap(long size) {
        long l = 0;
        if (isHuge(size)) {
            l = (long)(size * INITIAL_FILL_SIZE_FOR_HUGE);
        } else {
            l = (long)(size * INITIAL_FILL_SIZE_FOR_SMALL) + 2;
        }
        if (l > MAX_INITIAL_ENTRY_SLOTS) return MAX_INITIAL_ENTRY_SLOTS;
        assert l < Integer.MAX_VALUE;
        return (int)l;
    }

    private IntPairFloatMap allocateIntPairFloatMap(int leftSize, int rightSize) {
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

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void setScoreIfGreater(float score) {
        this.score = Math.max(score, this.score);
    }

    public float get(int A, int B) {
        return data.get(A, B);
    }

    public void set(int A, int B, float value) {
        data.put(A, B, value);
        assert data.get(A, B) > 0;
    }

    public IntPairFloatMap.ReturnType putIfGreater(int A, int B, float value) {
        return data.putIfGreater(A, B, value);
    }

    public float getJoinLeft(int A, int B) {
        return joinDataLeft.get(A, B);
    }

    public void setJoinLeft(int A, int B, float value) {
        joinDataLeft.put(A, B, value);
    }

    public IntPairFloatMap.ReturnType putJoinLeftIfGreater(int A, int B, float value) {
        return joinDataLeft.putIfGreater(A, B, value);
    }

    public float getJoinRight(int A, int B) {
        return joinDataRight.get(A, B);
    }

    public void setJoinRight(int A, int B, float value) {
        joinDataRight.put(A, B, value);
    }

    public IntPairFloatMap.ReturnType putJoinRightIfGreater(int A, int B, float value) {
        return joinDataRight.putIfGreater(A, B, value);
    }

    public float getMaxLeft(int B) {
        return maxLeft.get(B);
    }
    public float getMaxRight(int A) {
        return maxRight.get(A);
    }
    public float getMaxJoinLeft(int B) {
        return maxJoinLeft.get(B);
    }
    public float getMaxJoinRight(int A) {
        return maxJoinRight.get(A);
    }

    public IntFloatIterator eachInMaxLeft() {
        return maxLeft.entries();
    }
    public IntFloatIterator eachInMaxRight() {
        return maxRight.entries();
    }
    public IntFloatIterator eachInMaxJoinLeft() {
        return maxJoinLeft.entries();
    }
    public IntFloatIterator eachInMaxJoinRight() {
        return maxJoinRight.entries();
    }
    public IntPairFloatIterator each() {
        return data.entries();
    }
    public IntPairFloatIterator eachInJoinLeft() {
        return joinDataLeft.entries();
    }
    public IntPairFloatIterator eachInJoinRight() {
        return joinDataRight.entries();
    }


    public void putMaxLeftIfGreater(int B, float value) {
        maxLeft.putIfGreater(B, value);
    }

    public void putMaxRightIfGreater(int A, float value) {
        maxRight.putIfGreater(A, value);
    }

    public void putMaxJoinLeftIfGreater(int B, float value) {
        maxJoinLeft.putIfGreater(B, value);
    }

    public void putMaxJoinRightIfGreater(int A, float value) {
        maxJoinRight.putIfGreater(A, value);
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
            if (spec instanceof IntFloatHashMap || spec instanceof IntPairFloatHashMap)
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
