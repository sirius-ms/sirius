
package de.unijena.bioinf.treealign.dp;

import de.unijena.bioinf.treealign.Tree;

import java.util.Collections;

/**
 * maps each pair of vertices (u, v) to a dynamic programming table S[u,v](A, B)
 * @param <T> type of vertex
 */
class TreeMap<T> {

    private final Table<T>[] tables;
    private final Table<T> emptyTable;
    private final int colSize;
    private float score;

    @SuppressWarnings("unchecked")
    TreeMap(int leftSize, int rightSize) {
        this.tables = (Table<T>[])new Table[leftSize * rightSize];
        this.colSize = leftSize;
        this.score = Float.POSITIVE_INFINITY;
        this.emptyTable = new Table<T>(Collections.<Tree<T>>emptyList(), Collections.<Tree<T>>emptyList(), true);
    }

    float getScore() {
        return score;
    }

    void setScore(float score) {
        this.score = score;
    }

    Table<T> get(int left, int right) {
        if (left < 0 || right < 0) return emptyTable;
        return this.tables[left + right * colSize];
    }

    void set(int left, int right, Table<T> table) {
        this.tables[left + right * colSize] = table;
    }



}
