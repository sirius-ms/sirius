package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;

import java.util.Iterator;
import java.util.function.Predicate;

public interface InstIterProvider extends Iterator<Ms2Experiment> {
    default InstanceImportIteratorMS2Exp asInstanceIterator(ProjectSpaceManager projectSpace) {
        return new InstanceImportIteratorMS2Exp(this, projectSpace);
    }

    default InstanceImportIteratorMS2Exp asInstanceIterator(ProjectSpaceManager projectSpace, Predicate<CompoundContainer> compoundFilter) {
        return new InstanceImportIteratorMS2Exp(this, projectSpace, compoundFilter);
    }
}