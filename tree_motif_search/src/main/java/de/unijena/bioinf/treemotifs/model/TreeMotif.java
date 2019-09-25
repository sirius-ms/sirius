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
