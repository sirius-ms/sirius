package de.unijena.bioinf.babelms;


import de.unijena.bioinf.projectspace.sirius.CompoundContainer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * concatenate multiple experiment iterators to one.
 * Useful to combine new input files with an existing workspaces
 */
public class MultiSourceIterator implements Iterator<CompoundContainer> {
    private final Iterator<Iterator<CompoundContainer>> iterators;
    private Iterator<CompoundContainer> currentIterator;

    public MultiSourceIterator(Iterator<Iterator<CompoundContainer>> iterators) {
        this.iterators = iterators;
        if (this.iterators.hasNext())
            currentIterator = this.iterators.next();
    }

    public MultiSourceIterator(Iterable<Iterator<CompoundContainer>> iterators) {
        this(iterators.iterator());
    }

    public MultiSourceIterator(Iterator<CompoundContainer>... iterators) {
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
    public CompoundContainer next() {
        if (hasNext())
            return currentIterator.next();
        throw new NoSuchElementException("No Elements Left in MultiSource iterator!");
    }
}
