package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExportResultsAction extends AbstractAction {
    public ExportResultsAction() {
        super("Export Results");
        putValue(Action.LARGE_ICON_KEY, Icons.FOLDER_CLOSE_32);
        putValue(Action.SHORT_DESCRIPTION, "Export results to data file");
        setEnabled(checkEnabled());

        //Workspace Listener
        MainFrame.MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {
                setEnabled(checkEnabled());
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {

            }
        });
    }

    private boolean checkEnabled() {
        BasicEventList<ExperimentContainer> sl = Workspace.COMPOUNT_LIST;
        if (!sl.isEmpty()) {
            for (ExperimentContainer e : sl) {
                if (e.getSiriusComputeState() == ComputingStatus.COMPUTED) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        WorkspaceIO.exportResults();
    }
}
