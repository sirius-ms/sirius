package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.fingerid.storage.ConfigStorage.CONFIG_STORAGE;
import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class EditExperimentAction extends AbstractAction {

    public EditExperimentAction() {
        super("Edit Experiment");
        putValue(Action.SMALL_ICON, Icons.EDIT_16);
        putValue(Action.SHORT_DESCRIPTION, "Edit Selected Experiment");

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
        ExperimentContainer ec = MF.getCompoundListSelectionModel().getSelected().get(0);
        if (ec == null) return;
        String guiname = ec.getGUIName();
        LoadController lc = new LoadController(MF, ec, CONFIG_STORAGE);
        lc.showDialog();
        if (lc.getReturnValue() == ReturnValue.Success) {
            if (!ec.getGUIName().equals(guiname)) {
                Workspace.resolveCompundNameConflict(ec);
            }
        }
    }
}
