package de.unijena.bioinf.sirius.gui.table;

import de.unijena.bioinf.sirius.gui.structure.AbstractEDTBean;
import org.jdesktop.beans.AbstractBean;

import javax.swing.*;
import java.util.List;

public interface ActiveElementChangedListener<E extends AbstractEDTBean, D> {

    void resultsChanged(D experiment, E sre, List<E> resultElements, ListSelectionModel selections);

}
