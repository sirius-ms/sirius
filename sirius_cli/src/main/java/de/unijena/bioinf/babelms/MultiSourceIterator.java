package de.unijena.bioinf.babelms;


import de.unijena.bioinf.ms.frontend.subtools.Instance;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * concatenate multiple experiment iterators to one.
 * Useful to combine new input files with an existing workspaces
 */
public class MultiSourceIterator implements Iterator<Instance> {
    private final Iterator<Iterator<Instance>> iterators;
    private Iterator<Instance> currentIterator;

    public MultiSourceIterator(Iterator<Iterator<Instance>> iterators) {
        this.iterators = iterators;
        if (this.iterators.hasNext())
            currentIterator = this.iterators.next();
    }

    public MultiSourceIterator(Iterable<Iterator<Instance>> iterators) {
        this(iterators.iterator());
    }

    public MultiSourceIterator(Iterator<Instance>... iterators) {
        this(Arrays.asList(iterators));
    }


    @Override
    public boolean hasNext() {
        if (currentIterator == null)
            return false;
        if (currentIterator.hasNext())
            return true;
        if (iterators.hasNext()) {
            currentIterator = iterators.next();
            return hasNext();
        }

        return false;
    }

    @Override
    public Instance next() {
        if (hasNext())
            return currentIterator.next();
        throw new NoSuchElementException("No Elements Left in MultiSource iterator!");
    }
}
