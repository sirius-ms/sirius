
package de.unijena.bioinf.treealign;

import java.util.Iterator;

/**
 * @author Kai Dührkop
 */
public class AbstractBacktrace<T> implements Backtrace<T> {

    @Override
    public void deleteLeft(float score, T node) {
    }

    @Override
    public void deleteRight(float score, T node) {
    }

    @Override
    public void match(float score, T left, T right) {
    }

    @Override
    public void innerJoinLeft(T node) {

    }

    @Override
    public void innerJoinRight(T node) {

    }

    @Override
    public void matchVertices(float score, T left, T right) {
    }

    @Override
    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber) {
    }
}
