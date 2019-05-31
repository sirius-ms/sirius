package de.unijena.bioinf.ms.frontend;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.Arrays;
import java.util.Iterator;

/**
 * concatenate multiple experiment iterators to one.
 * Useful to combine new input files with an existing input with existing workspaces
 */
public class MultiSourceIterator implements Iterator<Ms2Experiment> {
    private final Iterator<Iterator<Ms2Experiment>> iterators;
    private Iterator<Ms2Experiment> currentIterator;

    public MultiSourceIterator(Iterator<Iterator<Ms2Experiment>> iterators) {
        this.iterators = iterators;
        if (this.iterators.hasNext())
            currentIterator = this.iterators.next();
    }

    public MultiSourceIterator(Iterable<Iterator<Ms2Experiment>> iterators) {
        this(iterators.iterator());
    }

    public MultiSourceIterator(Iterator<Ms2Experiment>... iterators) {
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
    public Ms2Experiment next() {
        if (hasNext())
            return currentIterator.next();
        return null;
    }
}
