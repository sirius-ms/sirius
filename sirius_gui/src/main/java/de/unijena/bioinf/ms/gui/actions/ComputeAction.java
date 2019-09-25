package de.unijena.bioinf.ms.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.ms.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeAction extends AbstractAction {
    public ComputeAction() {
        super("Compute");
        putValue(Action.SMALL_ICON, Icons.RUN_16);
        putValue(Action.LARGE_ICON_KEY, Icons.RUN_32);
        putValue(Action.SHORT_DESCRIPTION, "Compute selected compound(s)");

        setEnabled(!MF.getCompoundListSelectionModel().isSelectionEmpty());

        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentResultBean> event, DefaultEventSelectionModel<ExperimentResultBean> selection) {}

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentResultBean> selection) {
                setEnabled(!selection.isSelectionEmpty());
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!MF.getCompoundListSelectionModel().isSelectionEmpty()) {
            new BatchComputeDialog(MF, MF.getCompoundListSelectionModel().getSelected());
        }
    }
}