package de.unijena.bioinf.ms.gui.actions;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.compute.FingerIdDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;

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

        Jobs.runInBackground(() -> proofCSI(MainFrame.CONNECTION_MONITOR.checkConnection().isConnected()));

        MF.getCompoundList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
                if (MF.isFingerid()) {
                    for (InstanceBean container : event.getSourceList()) {
                        if (container.getResults().size() > 0) {
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
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
            }
        });

        MainFrame.CONNECTION_MONITOR.addConectionStateListener(evt -> {
            ConnectionMonitor.ConnectionState value = (ConnectionMonitor.ConnectionState) evt.getNewValue();
            setEnabled(proofCSI(value.equals(ConnectionMonitor.ConnectionState.YES)));
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (CheckConnectionAction.isConnectedAndLoad())
            return;


        final FingerIdDialog dialog = new FingerIdDialog(MF, MF.isFingerid(), false);
        final int returnState = dialog.run();
        final SearchableDatabase db = dialog.getSearchDb();
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


    protected boolean proofCSI(final boolean network) {
        setEnabled(false);
        if (MF.isFingerid() && MF.getCompounds().size() > 0) {
            if (network) {
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
