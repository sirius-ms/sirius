package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.util.List;

public interface ActiveResultChangedListener {

    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selections);

}
