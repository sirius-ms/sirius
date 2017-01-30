package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.COMPOUNT_LIST;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExportResultsAction extends AbstractAction {
    public ExportResultsAction() {
        super("Export Results");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_CLOSE_32);
        putValue(Action.SHORT_DESCRIPTION, "Export results to data file");
        setEnabled(!COMPOUNT_LIST.isEmpty());

        //Workspace Listener
        COMPOUNT_LIST.addListEventListener(new ListEventListener<ExperimentContainer>() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> listChanges) {
                EventList<ExperimentContainer> sl = listChanges.getSourceList();
                if (!sl.isEmpty()) {
                    for (ExperimentContainer e : sl) {
                        if (e.getComputeState() == ComputingStatus.COMPUTED) {
                            setEnabled(true);
                            return;
                        }
                    }
                }
                setEnabled(false);

            }
        });

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Workspace.exportResults();
    }
}
