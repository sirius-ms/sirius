package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.Iterator;

class IdentificationResultIterator implements Iterator<IdentificationResult> {
    private final Iterator<SiriusResultElement> sourceIt;

    IdentificationResultIterator(Iterator<SiriusResultElement> guiResults) {
        sourceIt = guiResults;
    }

    @Override
    public boolean hasNext() {
        return sourceIt.hasNext();
    }

    @Override
    public IdentificationResult next() {
        return sourceIt.next().getResult();
    }
}
