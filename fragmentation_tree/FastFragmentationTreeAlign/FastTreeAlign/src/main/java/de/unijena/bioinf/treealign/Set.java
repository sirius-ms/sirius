
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

package de.unijena.bioinf.treealign;

import java.util.*;

public class Set<S> implements Iterable<S> {

    private int bits;
    private List<S> basicSet;

    public static int of(List<?> list) {
        return (1<<list.size())-1;
    }

    public static void generateSubsetsUntil(int[][] subsets, int bits) {
        for (int i=0; i <= bits; ++i) {
            generateSubsets(subsets, i);
        }
    }

    public static void generateSubsets(int[][] superset, int set) {
        final int[] subsets = new int[1<<(Integer.bitCount(set))];
        superset[set] = subsets;
        int k=0;
        for (int i=1; i <= set; ++i) {
            if ((i & set) == i) {
                subsets[++k] = i;
            }
        }
    }

    public Set(Set<S> set) {
        this(set.basicSet, set.bits);
    }

    public Set(List<S> basicSet, int bits) {
        if (bits < 0) {
            throw new IllegalArgumentException("Illegal bitvector " + bits);
        }
        this.bits = bits;
        this.basicSet = basicSet;
    }

    public Set(List<S> basicSet) {
        this(basicSet, (1<<basicSet.size())-1);
    }

    private void checkCompatibility(Set<S> s) {
        if (s.basicSet != basicSet) {
            throw new IllegalArgumentException("Incompatible sets: " + this + " ( " + basicSet + " )  and " + s + " ( " + s.basicSet + " )");
        }
    }

    public int index() {
        return bits;
    }

    public Set<S> without(S elem) {
        final int index = asList().indexOf(elem);
        if (index < 0) throw new NoSuchElementException();
        return without(1<<index);
    }

    public Set<S> without(int key) {
        return new Set<S>(basicSet, bits & ~key);
    }

    public Set<S> with(S elem) {
        final int index = asList().indexOf(elem);
        if (index < 0) throw new NoSuchElementException();
        return with(1<<index);
    }

    public Set<S> with(int key) {
        return new Set<S>(basicSet, bits | key);
    }

    public Set<S> intersection(Set<S> s) {
        checkCompatibility(s);
        return new Set<S>(basicSet, bits & s.bits);
    }

    public Set<S> union(Set<S> s) {
        checkCompatibility(s);
        return new Set<S>(basicSet, bits | s.bits);
    }

    public Set<S> complement() {
        return new Set<S>(basicSet, ~bits);
    }

    public Set<S> difference(Set<S> s) {
        checkCompatibility(s);
        return new Set<S>(basicSet, bits & ~s.bits);
    }

    public List<S> asList() {
        if (bits == 0) return Collections.emptyList();
        final ArrayList<S> list = new ArrayList<S>(basicSet.size());
        int i = Integer.numberOfTrailingZeros(bits);
        int b = 1<<i;
        while (b <= bits) {
            if ((b & bits) == b) list.add(basicSet.get(i));
            b = b << 1;
            if (++i > 32) break;
        }
        return list;
    }

    public static <S> List<S> subList(List<S> list, int bits) {
        if (bits == 0) return Collections.emptyList();
        final ArrayList<S> ary = new ArrayList<S>(Integer.bitCount(bits));
        //int i = Integer.numberOfTrailingZeros(bits);
        int i = 0;
        int b = 1;
        while (b <= bits) {
            if ((b & bits) == b) ary.add(list.get(i));
            b = b << 1;
            if (++i > 32) break;
        }
        return ary;
    }

    public Iterator<S> iterator() {
        return new SetIterator<S>(this);
    }

    public boolean equals(Object o) {
        if (o instanceof Set) return equals((Set)o);
        return false;
    }

    public boolean equals(Set s) {
        return basicSet == s.basicSet && bits == s.bits;
    }

    public int hashCode() {
        return bits;
    }

    @Override
    public String toString() {
        return asList().toString();
    }

    public static class SetIterator<S> implements Iterator<S> {

        private final int bits;
        private final List<S> basicSet;
        private int index;
        private int value;

        public SetIterator(Set<S> set) {
            this.bits = set.bits;
            this.basicSet = set.basicSet;
            this.index = Integer.numberOfTrailingZeros(bits);
            this.value = 1<<index;
        }

        public boolean hasNext() {
            return (value & bits) == value;
        }

        public S next() {
            final S elem = basicSet.get(index);
            do {
                ++index;
                value = value<<1;
            } while(value <= bits && (value & bits) != value);
            return elem;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
