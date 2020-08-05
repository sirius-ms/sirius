
package de.unijena.bioinf.treealign;

import java.util.Iterator;

public interface Backtrace<T> {

    public void deleteLeft(float score, T node);

    public void deleteRight(float score, T node);

    public void match(float score, T left, T right);

    public void innerJoinLeft(T node);

    public void innerJoinRight(T node);

    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber);

    public void matchVertices(float score, T left, T right);

}
