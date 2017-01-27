package de.unijena.bioinf.sirius.gui.mainframe;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import javafx.collections.ListChangeListener;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface ExperimentListChangeListener{

    void listChanged(ExperimentListChangeEvent listChanges);
}
