package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;

import java.util.Iterator;

class InstanceIteratorMS2Exp implements Iterator<Instance> {

    private final ProjectSpaceManager spaceManager;
    private final MS2ExpInputIterator MS2ExpInputIterator;


    public InstanceIteratorMS2Exp(MS2ExpInputIterator MS2ExpInputIterator, ProjectSpaceManager spaceManager) {
        this.MS2ExpInputIterator = MS2ExpInputIterator;
        this.spaceManager = spaceManager;
    }

    @Override
    public boolean hasNext() {
        return MS2ExpInputIterator.hasNext();
    }

    @Override
    public Instance next() {
        Ms2Experiment input = MS2ExpInputIterator.next();
        if (input == null) return null;
        return new Instance(input, spaceManager); //this writers
    }
}
