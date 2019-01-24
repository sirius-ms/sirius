package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.projectspace.GuiProjectSpace;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.dialogs.CloseDialogNoSaveReturnValue;
import de.unijena.bioinf.sirius.gui.dialogs.CloseDialogReturnValue;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class DeleteExperimentAction extends AbstractAction {
    public static final String NEVER_ASK_AGAIN_KEY = PropertyManager.PROPERTY_BASE + ".sirius.dialog.delete_experiment_action.ask_again";

    public DeleteExperimentAction() {
        super("Delete");
        putValue(Action.SMALL_ICON, Icons.REMOVE_DOC_16);
        putValue(Action.SHORT_DESCRIPTION, "Delete the selected data");

        setEnabled(!MF.getCompoundListSelectionModel().isSelectionEmpty());

        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {
                setEnabled(!selection.isSelectionEmpty());
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!PropertyManager.getBooleanProperty(NEVER_ASK_AGAIN_KEY)) {
            CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(MF, "When removing the selected compound(s) you will loose all computed identification results?", NEVER_ASK_AGAIN_KEY);
            CloseDialogReturnValue val = diag.getReturnValue();
            if (val == CloseDialogReturnValue.abort) return;
        }

        List<ExperimentContainer> toRemove = new ArrayList<>(MF.getExperimentList().getCompoundListSelectionModel().getSelected());
        for (ExperimentContainer cont : toRemove) {
            Jobs.cancel(cont);
            GuiProjectSpace.PS.remove(cont);
        }
    }
}
