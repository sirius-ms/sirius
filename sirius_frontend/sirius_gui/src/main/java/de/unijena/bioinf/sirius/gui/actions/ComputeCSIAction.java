package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.fingerid.FingerIdDialog;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeCSIAction extends AbstractAction {

    public ComputeCSIAction() {
        super("CSI:FingerId");
        putValue(Action.SMALL_ICON, Icons.FINGER_32);
        putValue(Action.SHORT_DESCRIPTION, "Search computed Experiments with CSI:FingerID");

        proofCSI();

        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {
                if (MF.getCsiFingerId().isEnabled()) {
                    for (ExperimentContainer container : event.getSourceList()) {
                        if (container.isComputed()) {
                            setEnabled(true);
                            return;
                        }
                    }
                    setEnabled(false);
                } else {
                    setEnabled(false);
                }
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {}
        });

        MF.getCsiFingerId().getEnabledListeners().add(new Runnable() {
            @Override
            public void run() {
                proofCSI();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!MainFrame.MF.csiConnectionAvailable())
            return;

        final FingerIdDialog dialog = new FingerIdDialog(MF, MF.getCsiFingerId(), true, false);
        final int returnState = dialog.run();

        if (returnState == FingerIdDialog.COMPUTE_ALL) {
            MF.getCsiFingerId().computeAll(MF.getCompounds());
        } else if (returnState == FingerIdDialog.COMPUTE) {
            MF.getCsiFingerId().computeAll(MF.getCompoundListSelectionModel().getSelected());
        }
    }

    protected void proofCSI() {
        setEnabled(false);
        if (MF.getCsiFingerId().isEnabled() && MF.csiConnectionAvailable() && MF.getCompounds().size() > 0) {
            for (ExperimentContainer container : MF.getCompounds()) {
                if (container.isComputed())
                    setEnabled(true);
                break;
            }
        }
    }
}
