package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.sirius.gui.dialogs.CloseDialogNoSaveReturnValue;
import de.unijena.bioinf.sirius.gui.dialogs.CloseDialogReturnValue;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.CONFIG_STORAGE;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class DeleteExperimentAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        List<ExperimentContainer> toRemove = MF.getCompountListPanel().getCompoundListView().getSelectedValuesList();

        if (!CONFIG_STORAGE.isCloseNeverAskAgain()) {
            CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(MF, "When removing the selected experiment(s) you will loose all computed identification results?");
            CloseDialogReturnValue val = diag.getReturnValue();
            if (val == CloseDialogReturnValue.abort) return;
        }
        for (ExperimentContainer cont : toRemove) {
            MF.getBackgroundComputation().cancel(cont);
        }
        Workspace.removeAll(toRemove);
    }
}
