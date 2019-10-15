package de.unijena.bioinf.ms.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.io.projectspace.GuiProjectSpace;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.babelms.load.LoadController;
import de.unijena.bioinf.ms.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class EditExperimentAction extends AbstractAction {

    public EditExperimentAction() {
        super("Edit");
        putValue(Action.SMALL_ICON, Icons.EDIT_16);
        putValue(Action.SHORT_DESCRIPTION, "Edit the selected data");

        setEnabled(!MF.getCompoundListSelectionModel().isSelectionEmpty());

        MF.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
                setEnabled(!selection.isSelectionEmpty());
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        InstanceBean ec = MF.getCompoundListSelectionModel().getSelected().get(0);
        if (ec == null) return;
        String guiname = ec.getGUIName();
        LoadController lc = new LoadController(MF, ec);
        lc.showDialog();
        if (!ec.getGUIName().equals(guiname)) {
            GuiProjectSpace.PS.addName(ec);
        }
    }
}
