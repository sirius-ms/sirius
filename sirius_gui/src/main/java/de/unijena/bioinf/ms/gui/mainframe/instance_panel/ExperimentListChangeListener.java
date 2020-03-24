package de.unijena.bioinf.ms.gui.mainframe.instance_panel;/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 26.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.projectspace.InstanceBean;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public interface ExperimentListChangeListener{
    void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection);
    void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection);
}
