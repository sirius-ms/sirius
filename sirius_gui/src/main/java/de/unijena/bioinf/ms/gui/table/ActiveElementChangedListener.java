package de.unijena.bioinf.ms.gui.table;

import de.unijena.bioinf.ms.frontend.core.SiriusPCS;

import javax.swing.*;
import java.util.List;

public interface ActiveElementChangedListener<E extends SiriusPCS, D> {

    void resultsChanged(D experiment, E sre, List<E> resultElements, ListSelectionModel selections);

}
