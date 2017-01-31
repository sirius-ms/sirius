package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import de.unijena.bioinf.sirius.gui.mainframe.ExperimentListChangeListener;
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

        setEnabled(!MF.getCompoundView().isSelectionEmpty() && MF.getCompoundView().getSelectedValue().isComputing());

        MF.getCompountListPanel().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, JList<ExperimentContainer> source) {
                if (!event.getSourceList().isEmpty()) {
                    for (ExperimentContainer ec : source.getSelectedValuesList()) {
                        if (ec != null && (ec.isComputing() || ec.isQueued())) { //todo minor but: isQueued is somehow not detected -> maybe not property change fired?
                            setEnabled(true);
                            return;
                        }
                    }
                }
                setEnabled(false);
            }

            @Override
            public void listSelectionChanged(JList<ExperimentContainer> source) {

            }
        });

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        for (ExperimentContainer ec : MF.getCompoundView().getSelectedValuesList()) {
            MF.getBackgroundComputation().cancel(ec);
        }
    }


}
