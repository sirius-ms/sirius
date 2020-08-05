
package de.unijena.bioinf.treealign.scoring;

import de.unijena.bioinf.util.Iterators;

import java.util.Iterator;

/**
 * @author Kai Dührkop
 */
public abstract class AbstractMultijoinScoring<T> extends AbstractScoring<T> {

    public abstract float join(Iterator<T> leftNodes, Iterator<T> rightNodes, int leftSize, int rightSize);

    protected float joinOperation(T parent, T join, T other) {
        return join(Iterators.pair(join, parent), Iterators.singleton(other), 2, 1);
    }
    
}
