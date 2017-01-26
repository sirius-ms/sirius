package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

public interface ActiveResultChangedListener {

    public void resultsChanged(ExperimentContainer ec, SiriusResultElement sre);

}
