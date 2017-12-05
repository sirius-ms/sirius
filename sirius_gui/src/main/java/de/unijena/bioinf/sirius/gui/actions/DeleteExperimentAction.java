package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.dialogs.CloseDialogNoSaveReturnValue;
import de.unijena.bioinf.sirius.gui.dialogs.CloseDialogReturnValue;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.fingerid.storage.ConfigStorage.CONFIG_STORAGE;
import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class DeleteExperimentAction extends AbstractAction {
    public DeleteExperimentAction() {
        super("Remove Experiment(s)");
        putValue(Action.SMALL_ICON, Icons.REMOVE_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "Remove selected Experiment(s)");

        setEnabled(!MF.getCompoundListSelectionModel().isSelectionEmpty());

        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {}

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {
                setEnabled(!selection.isSelectionEmpty());
            }
        });
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!CONFIG_STORAGE.isCloseNeverAskAgain()) {
            CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(MF, "When removing the selected experiment(s) you will loose all computed identification results?");
            CloseDialogReturnValue val = diag.getReturnValue();
            if (val == CloseDialogReturnValue.abort) return;
        }

        List<ExperimentContainer> toRemove = new ArrayList<>(MF.getExperimentList().getCompoundListSelectionModel().getSelected());
        for (ExperimentContainer cont : toRemove) {
            MF.getBackgroundComputation().cancel(cont);
        }
        Workspace.removeAll(toRemove);
    }
}
