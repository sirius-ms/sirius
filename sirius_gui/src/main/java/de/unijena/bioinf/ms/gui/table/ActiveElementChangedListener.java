package de.unijena.bioinf.ms.gui.table;

import de.unijena.bioinf.sirius.core.AbstractEDTBean;

import javax.swing.*;
import java.util.List;

public interface ActiveElementChangedListener<E extends AbstractEDTBean, D> {

    void resultsChanged(D experiment, E sre, List<E> resultElements, ListSelectionModel selections);

}
