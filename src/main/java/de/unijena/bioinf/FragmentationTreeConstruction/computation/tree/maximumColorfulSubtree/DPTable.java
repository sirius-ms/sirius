package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;

import java.util.Arrays;

class DPTable {

    final int[] keys;
    final int bitset;
    final int color;
    private double[] scores;
    final int vertexBit;
    private double opt;

    DPTable(MaximumColorfulSubtreeAlgorithm algo, int vertexColor, int bitset) {
        this.vertexBit = (1<<vertexColor);
        final int bitsetWithoutVertex = (bitset & ~vertexBit);
        this.keys = algo.subsetsFor(bitsetWithoutVertex);
        this.bitset = bitset;
        this.color = vertexColor;
        this.scores = null; // initialize lazy
        this.opt = Double.NEGATIVE_INFINITY;
    }

    boolean update(int bitset, double score) {
        if (score >= 0) {
            allocScores();
            final int key = Arrays.binarySearch(keys, bitset & ~vertexBit);
            if (key < 0) {
                assert false;
            }
            assert key >= 0;
            if (scores[key] > score) return false;
            scores[key] = score;
            this.opt = Math.max(opt, score);
            return true;
        } else return false;
    }

    double bestScore() {
        return opt;
    }

    double get(int bitset) {
        final int set = bitset & ~vertexBit;
        if (set == 0) return 0d;
        final int key = Arrays.binarySearch(keys, set);
        assert key >= 0;
        return (scores == null) ? Double.NEGATIVE_INFINITY : scores[key];
    }

    double getDirect(int index) {
        return (scores == null) ? Double.NEGATIVE_INFINITY : scores[index];
    }

    private void allocScores() {
        if (scores == null) {
            scores = new double[keys.length];
            Arrays.fill(scores, Double.NEGATIVE_INFINITY);
        }
    }


}
