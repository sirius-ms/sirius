package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree;

import gnu.trove.map.hash.TIntDoubleHashMap;

class DPTable {

    final int[] keys;
    final int bitset;
    final int color;
    private TIntDoubleHashMap2 scores;
    final int vertexBit;
    private double opt;

    DPTable(MaximumColorfulSubtreeAlgorithm algo, int vertexColor, int bitset) {
        this.vertexBit = (1<<vertexColor);
        final int bitsetWithoutVertex = (bitset & ~vertexBit);
        this.keys = algo.subsetsFor(bitsetWithoutVertex);
        this.bitset = bitset;
        this.color = vertexColor;
        this.scores = null; // initialize lazy
        this.opt = 0;
    }

    boolean update(int bitset, double score) {
        if (score > 0) {
            allocScores();
            scores.putIfAbsent(bitset, score);
            this.opt = Math.max(opt, score);
            return true;
        } else {
            return false;
        }
    }

    double bestScore() {
        return opt;
    }

    double get(int bitset) {
        final int set = bitset & ~vertexBit;

        if (set == 0 || scores == null) return 0d;
        else return scores.get(set);
        /*
        final int key = Arrays.binarySearch(keys, set);
        assert key >= 0;
        return (scores == null) ? Double.NEGATIVE_INFINITY : scores.get(key);
        */
    }
    /*
    double getDirect(int index) {
        return (scores == null) ? Double.NEGATIVE_INFINITY : scores[index];
    }
    */

    private void allocScores() {
        if (scores == null) {
            scores = new TIntDoubleHashMap2();
            //scores = new double[keys.length];
            //Arrays.fill(scores, Double.NEGATIVE_INFINITY);
        }
    }


    private static class TIntDoubleHashMap2 extends TIntDoubleHashMap {
        public TIntDoubleHashMap2() {
            super(20, TIntDoubleHashMap.DEFAULT_LOAD_FACTOR, 0, 0);
        }

        boolean putIfGreater(int key, double score) {
            int index = insertKey( key );
            if (index < 0) {
                final int k = -index - 1;
                if (_values[k] >= score) {
                    return false;
                }
                _values[k] = score;
                return true;
            } else {
                doPut(key, score, index);
                return true;
            }
        }
        private double doPut( int key, double value, int index ) {
            double previous = no_entry_value;
            boolean isNewMapping = true;
            if ( index < 0 ) {
                index = -index -1;
                previous = _values[index];
                isNewMapping = false;
            }
            _values[index] = value;

            if (isNewMapping) {
                postInsertHook( consumeFreeSlot );
            }

            return previous;
        }
    }


}
