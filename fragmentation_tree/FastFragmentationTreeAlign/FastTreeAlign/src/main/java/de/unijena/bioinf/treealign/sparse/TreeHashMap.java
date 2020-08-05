
package de.unijena.bioinf.treealign.sparse;

import de.unijena.bioinf.treealign.Tree;

import java.util.Collections;

class TreeHashMap<T> {
    private final HashTable<T>[] tables;
    private final HashTable<T> emptyTable;
    private final int colSize;
    private float score;

    TreeHashMap(int leftSize, int rightSize) {
        this.tables = new HashTable[leftSize * rightSize];
        this.colSize = leftSize;
        this.score = Float.POSITIVE_INFINITY;
        this.emptyTable = new HashTable<T>(Collections.<Tree<T>>emptyList(), Collections.<Tree<T>>emptyList(), true);
    }

    // TODO: REMOVE
    float getScore() {
        return score;
    }

    void setScore(float score) {
        this.score = score;
    }

    HashTable<T> get(int left, int right) {
        if (left < 0 || right < 0) return emptyTable;
        return this.tables[left + right * colSize];
    }

    void set(int left, int right, HashTable<T> table) {
        this.tables[left + right * colSize] = table;
    }
}
