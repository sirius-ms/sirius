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

package de.unijena.bioinf.treemotifs.model;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongDoubleHashMap;

class TreeMotif {
    private final String name;
    private final long[] fragments, rootLosses;

    TreeMotif(String name, long[] fragments, long[] rootLosses) {
        this.name = name;
        this.fragments = fragments;
        this.rootLosses = rootLosses;
    }

    public String getName() {
        return name;
    }

    long[] getFragments() {
        return fragments;
    }

    long[] getRootLosses() {
        return rootLosses;
    }

    static double getRandomProbability(TLongDoubleHashMap probs, long[] a, long[] b) {
        int i=0, j=0;
        double shared = 0d;
        while (i < a.length && j < b.length) {
            if (a[i] > b[j]) {
                ++j;
            } else if (a[i] < b[j]) {
                ++i;
            } else {
                shared += probs.get(a[i]);
                ++i;
                ++j;
            }
        }
        return shared;
    }

    public long[] getSharedFragments(TreeMotif other) {
        return getShared(fragments, other.fragments);
    }
    public long[] getSharedRootLosses(TreeMotif other) {
        return getShared(rootLosses, other.rootLosses);
    }

    public int numberOfSharedFragments(TreeMotif other) {
        return shared(fragments,other.fragments);
    }
    public int numberOfSharedRootLosses(TreeMotif other) {
        return shared(rootLosses,other.rootLosses);
    }

    public int numberOfSharedFormulas(TreeMotif other) {
        return numberOfSharedFragments(other) + numberOfSharedRootLosses(other);
    }

    private static int shared(long[] a, long[] b) {
        int i=0, j=0, shared = 0;
        while (i < a.length && j < b.length) {
            if (a[i] > b[j]) {
                ++j;
            } else if (a[i] < b[j]) {
                ++i;
            } else {
                ++i;
                ++j;
                ++shared;
            }
        }
        return shared;
    }
    private static long[] getShared(long[] a, long[] b) {
        int i=0, j=0;
        final TLongArrayList buffer = new TLongArrayList(10);
        while (i < a.length && j < b.length) {
            if (a[i] > b[j]) {
                ++j;
            } else if (a[i] < b[j]) {
                ++i;
            } else {
                buffer.add(a[i]);
                ++i;
                ++j;
            }
        }
        return buffer.toArray();
    }
}
