package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;

import java.util.Iterator;

public interface InstIterProvider extends Iterator<Ms2Experiment> {
    default InstanceIteratorMS2Exp asInstanceIterator(ProjectSpaceManager projectSpace) {
        return new InstanceIteratorMS2Exp(this, projectSpace);
    }
}