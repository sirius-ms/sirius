
package de.unijena.bioinf.treealign.scoring;

import java.util.Iterator;

/**
 * Simple symetric singlejoin scoring with constant gap scores
 * @author Kai Dührkop
 */
public abstract class AbstractScoring<T> implements Scoring<T> {
    
    protected abstract float joinOperation(T parent, T join, T other);
    protected abstract float matchOperation(T left, T right);
    protected abstract float gapScore();

    @Override
    public float joinLeft(T left, T join, T right) {
        return joinOperation(left, join, right);
    }

    @Override
    public float joinRight(T right, T join, T left) {
        return joinOperation(right, join, left);
    }

    @Override
    public float match(T left, T right) {
        return matchOperation(left, right);
    }

    @Override
    public float deleteLeft(T left) {
        return gapScore();
    }

    @Override
    public float deleteRight(T right) {
        return gapScore();
    }

    @Override
    public float join(Iterator<T> leftNodes, Iterator<T> rightNodes, int leftSize, int rightSize) {
        throw new UnsupportedOperationException("This scoring doesn't support multijoin scoring");
    }

    @Override
    public float scoreVertices(T left, T right) {
        return 0;
    }
}
