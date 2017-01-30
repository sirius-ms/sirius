package de.unijena.bioinf.sirius.gui.mainframe;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface ExperimentListChangeListener{
    void listChanged(ListEvent<ExperimentContainer> event, JList<ExperimentContainer> source);
    void listSelectionChanged(JList<ExperimentContainer> source);
}
