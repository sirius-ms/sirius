package de.unijena.bioinf.ms.gui.actions;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.compute.FingerIdDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeCSIAction extends AbstractAction {

    public ComputeCSIAction() {
        super("CSI:FingerID");
        putValue(Action.SMALL_ICON, Icons.FINGER_32);
        putValue(Action.SHORT_DESCRIPTION, "Search computed compounds with CSI:FingerID");

        Jobs.runInBackground(() -> proofCSI(CheckConnectionAction.checkConnectionAndLoad().state));

        MF.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
                for (InstanceBean container : event.getSourceList()) {
                    if (container.getResults().size() > 0) {
                        setEnabled(true);
                        return;
                    }
                }
                setEnabled(false);
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
            }
        });

        MainFrame.CONNECTION_MONITOR.addConectionStateListener(evt -> {
            ConnectionMonitor.ConnectionState value = (ConnectionMonitor.ConnectionState) evt.getNewValue();
            proofCSI(value);
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final FingerIdDialog dialog = new FingerIdDialog(MF, CheckConnectionAction.isConnectedAndLoad(), false);
        final int returnState = dialog.run();
        if (returnState == FingerIdDialog.COMPUTE_ALL) {
            System.out.println("Compute ALL CSI not implemented yet!");
            //MF.getCsiFingerId().computeAll(MF.getCompounds(), db);
//            MF.getCsiFingerId().computeAll(MF.getCompounds(), db);

        } else if (returnState == FingerIdDialog.COMPUTE) {
            System.out.println("Compute CSI not implemented yet!");
            //MF.getCsiFingerId().computeAll(MF.getCompoundListSelectionModel().getSelected(), db);
//            MF.getCsiFingerId().computeAll(MF.getCompoundListSelectionModel().getSelected(), db);
        }

    }


    protected boolean proofCSI(final ConnectionMonitor.ConnectionState networkCheck) {
        setEnabled(false);
        if (MF.getCompounds().size() > 0) {
            if (!networkCheck.equals(ConnectionMonitor.ConnectionState.NO)) {
                for (InstanceBean container : MF.getCompounds()) {
                    if (container.getResults().size() > 0)
                        return true;
                    setEnabled(true);
                    break;
                }
            }
        }
        return false;
    }
}
