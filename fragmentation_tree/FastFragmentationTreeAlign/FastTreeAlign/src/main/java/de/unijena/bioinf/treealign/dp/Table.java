
package de.unijena.bioinf.treealign.dp;

import de.unijena.bioinf.treealign.Set;
import de.unijena.bioinf.treealign.Tree;

import java.util.List;

/**
 * The dynamic programming table S(A,B) for a given pair of vertices (u, v)
 */
class Table<T> {

    private final float[] data;
    private final float[] joinDataLeft;
    private final float[] joinDataRight;
    private final int colSize;
    private final List<Tree<T>> basicSetA;
    private final List<Tree<T>> basicSetB;

    Table(List<Tree<T>> basicSetA, List<Tree<T>> basicSetB, boolean useJoins) {
        this.basicSetA = basicSetA;
        this.basicSetB = basicSetB;
        this.colSize = 1<<basicSetA.size();
        this.data = new float[(1<<basicSetA.size()) * (1<<basicSetB.size())];
        if (useJoins) {
            this.joinDataLeft = new float[data.length];
            this.joinDataRight = new float[data.length];
        } else {
            this.joinDataLeft = null;
            this.joinDataRight = null;
        }
    }

    int cols() {
        return colSize;
    }

    int rows() {
        return 1<<basicSetB.size();
    }

    void setPrejoinLeft(Set<Tree<T>> a, Set<Tree<T>> b, float value) {
        setPrejoinLeft(a.index(), b.index(), value);
    }

    void setPrejoinLeft(int a, int b, float value) {
        this.joinDataLeft[a + b*colSize] = value;
    }

    float getPrejoinLeft(Set<Tree<T>> a, Set<Tree<T>> b) {
        return getPrejoinLeft(a.index(), b.index());
    }

    float getPrejoinLeft(int a, int b) {
        return this.joinDataLeft[a + b*colSize];
    }

    void setPrejoinRight(Set<Tree<T>> a, Set<Tree<T>> b, float value) {
        setPrejoinRight(a.index(), b.index(), value);
    }

    void setPrejoinRight(int a, int b, float value) {
        this.joinDataRight[a + b*colSize] = value;
    }

    float getPrejoinRight(Set<Tree<T>> a, Set<Tree<T>> b) {
        return getPrejoinRight(a.index(), b.index());
    }

    float getPrejoinRight(int a, int b) {
        return this.joinDataRight[a + b*colSize];
    }

    float getScore() {
        return data[data.length-1];
    }

    float get(Set<Tree<T>> a, Set<Tree<T>> b) {
        return get(a.index(), b.index());
    }
    float get(int a, int b) {
        try {
            return this.data[a + b * colSize];
        } catch (ArrayIndexOutOfBoundsException exc) {
            throw new IndexOutOfBoundsException(a + "/" + b + "  " + basicSetA + ", " + basicSetB);
        }
    }

    void set(Set<Tree<T>> a, Set<Tree<T>> b, float score) {
        this.set(a.index(), b.index(), score);
    }

    void set(int a, int b, float score) {
        this.data[a + b * colSize] = score;
    }

}
