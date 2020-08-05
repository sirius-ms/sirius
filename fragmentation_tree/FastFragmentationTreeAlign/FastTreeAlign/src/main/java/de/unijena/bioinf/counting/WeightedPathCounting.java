
package de.unijena.bioinf.counting;

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.treealign.Tree;
import de.unijena.bioinf.treealign.scoring.SimpleEqualityScoring;

import java.util.ArrayList;

public class WeightedPathCounting<T> {

    protected final long[][] S, E;
    private final ArrayList<Tree<T>> leftVertices;
    private final ArrayList<Tree<T>> rightVertices;
    private final Weighting<T> weights;
    private final TreeAdapter<T> adapter;
    private final Tree<T> left;
    private final Tree<T> right;
    private final SimpleEqualityScoring<T> scoring;

    public WeightedPathCounting(SimpleEqualityScoring<T> scoring, Weighting<T> weighting, T left, T right, TreeAdapter<T> adapter) {
        this.adapter = adapter;
        this.leftVertices = new ArrayList<Tree<T>>();
        this.rightVertices = new ArrayList<Tree<T>>();
        final CompleteTreeDecorator<T> leftDeco = new CompleteTreeDecorator<T>(leftVertices);
        final CompleteTreeDecorator<T> rightDeco = new CompleteTreeDecorator<T>(rightVertices);
        this.left = new PostOrderTraversal<T>(left, adapter).call(leftDeco);
        this.right = new PostOrderTraversal<T>(right, adapter).call(rightDeco);
        this.S = new long[leftVertices.size()][rightVertices.size()];
        this.E = new long[leftVertices.size()][rightVertices.size()];
        this.weights = weighting;
        this.scoring = scoring;
    }


    public double compute() {
        computeE();
        computeS();
        double score = 0d;
        for (int i = 0; i < leftVertices.size(); ++i) {
            final Tree<T> u = leftVertices.get(i);
            for (int j = 0; j < rightVertices.size(); ++j) {
                final Tree<T> v = rightVertices.get(j);
                for (Tree<T> a : u.children()) {
                    for (Tree<T> b : v.children()) {
                        if (E[a.index][b.index] > 0) {
                            score += (S[a.index][b.index] + E[a.index][b.index] + 1) * weights.weight(a.label, b.label);
                        }
                    }
                }
            }
        }
        return score;
    }

    public void computeE() {
        // TODO: vermutlich ein Fehler. Root müsste Index E[n][m] haben
        E[0][0] = 0; // initialization
        for (int i = leftVertices.size() - 2; i >= 0; --i) {
            final Tree<T> u = leftVertices.get(i);
            for (int j = rightVertices.size() - 2; j >= 0; --j) {
                final Tree<T> v = rightVertices.get(j);
                if (scoring.isMatching(u.label, v.label)) {
                    E[u.index][v.index] = E[u.getParentIndex()][v.getParentIndex()] + 1;
                }
            }
        }
    }

    public void computeS() {
        for (int i = 0; i < leftVertices.size(); ++i) {
            final Tree<T> u = leftVertices.get(i);
            for (int j = 0; j < rightVertices.size(); ++j) {
                final Tree<T> v = rightVertices.get(j);
                long counter = 0;
                for (Tree<T> a : u.children()) {
                    for (Tree<T> b : v.children()) {
                        if (scoring.isMatching(a.label, b.label)) {
                            counter += (1 + ((a.index < 0 || b.index < 0) ? 0 : S[a.index][b.index]));
                        }
                    }
                }
                S[u.index][v.index] = counter;
            }
        }
    }
}
