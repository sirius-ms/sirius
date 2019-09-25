package de.unijena.bioinf.ms.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CancelComputeAction extends AbstractAction {
    public CancelComputeAction() {
        super("Cancel Computation");
        putValue(Action.SMALL_ICON, Icons.CANCEL_16);
        putValue(Action.LARGE_ICON_KEY, Icons.CANCEL_32);
        putValue(Action.SHORT_DESCRIPTION, "Cancel the running Computation(s) of the selected compound(s)");

        setEnabled(!MF.getCompoundListSelectionModel().isSelectionEmpty() && MF.getCompoundListSelectionModel().getSelected().get(0).isComputing());

        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentResultBean> event, DefaultEventSelectionModel<ExperimentResultBean> selection) {
                if (!selection.isSelectionEmpty()) {
                    for (ExperimentResultBean ec : selection.getSelected()) {
                        if (ec != null && (ec.isComputing() || ec.isQueued())) { //todo minor but: isQueued is somehow not detected -> maybe not property change fired?
                            setEnabled(true);
                            return;
                        }
                    }
                }
                setEnabled(false);
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentResultBean> selection) {

            }
        });

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        for (ExperimentResultBean ec : MF.getCompoundListSelectionModel().getSelected()) {
            Jobs.cancel(ec);
        }
    }


}
