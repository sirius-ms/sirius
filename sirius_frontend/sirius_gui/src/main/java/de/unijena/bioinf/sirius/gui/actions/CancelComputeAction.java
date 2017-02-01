package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CancelComputeAction extends AbstractAction {
    public CancelComputeAction() {
        super("Cancel Computation");
        putValue(Action.SMALL_ICON, Icons.CANCEL_16);
        putValue(Action.LARGE_ICON_KEY, Icons.CANCEL_32);
        putValue(Action.SHORT_DESCRIPTION, "Cancel the running Computation(s) of the selected Experiment(s)");

        setEnabled(!MF.getCompoundListSelectionModel().isSelectionEmpty() && MF.getCompoundListSelectionModel().getSelected().get(0).isComputing());

        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {
                if (!selection.isSelectionEmpty()) {
                    for (ExperimentContainer ec : selection.getSelected()) {
                        if (ec != null && (ec.isComputing() || ec.isQueued())) { //todo minor but: isQueued is somehow not detected -> maybe not property change fired?
                            setEnabled(true);
                            return;
                        }
                    }
                }
                setEnabled(false);
            }
            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {

            }
        });

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        for (ExperimentContainer ec : MF.getCompoundListSelectionModel().getSelected()) {
            MF.getBackgroundComputation().cancel(ec);
        }
    }


}
