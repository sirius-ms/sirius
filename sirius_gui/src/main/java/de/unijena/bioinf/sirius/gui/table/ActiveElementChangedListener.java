package de.unijena.bioinf.sirius.gui.table;

import org.jdesktop.beans.AbstractBean;

import javax.swing.*;
import java.util.List;

public interface ActiveElementChangedListener<E extends AbstractBean, D> {

    void resultsChanged(D experiment, E sre, List<E> resultElements, ListSelectionModel selections);

}
