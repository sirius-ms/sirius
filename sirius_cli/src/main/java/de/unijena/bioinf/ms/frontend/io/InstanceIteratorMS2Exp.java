package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;

import java.util.Iterator;
import java.util.function.Predicate;

class InstanceIteratorMS2Exp implements Iterator<Instance> {

    private final ProjectSpaceManager spaceManager;
    private final Iterator<Ms2Experiment> ms2ExperimentIterator;


    public InstanceIteratorMS2Exp(Iterator<Ms2Experiment> ms2ExperimentIterator, ProjectSpaceManager spaceManager) {
        this.ms2ExperimentIterator = ms2ExperimentIterator;
        this.spaceManager = spaceManager;
    }


    @Override
    public boolean hasNext() {
        return ms2ExperimentIterator.hasNext();
    }

    @Override
    public Instance next() {
        Ms2Experiment input = ms2ExperimentIterator.next();
        if (input == null) return null;
        return spaceManager.newCompoundWithUniqueId(input); //this writers
    }

    public void importAll() {
        while (hasNext())
            next();
    }
}
