package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

public interface ActiveResultChangedListener {

    public void resultsChanged(ExperimentContainer ec, SiriusResultElement sre);

}
