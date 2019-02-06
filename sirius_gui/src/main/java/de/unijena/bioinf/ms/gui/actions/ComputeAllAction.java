package de.unijena.bioinf.ms.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeAllAction extends AbstractAction {
    private static AtomicBoolean isActive = new AtomicBoolean(false);

    public ComputeAllAction() {
        super();
        computationCanceled();
        setEnabled(false);

        //filtered Workspace Listener
        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentResultBean> event, DefaultEventSelectionModel<ExperimentResultBean> selection) {
                setEnabled(event.getSourceList().size() > 0);
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentResultBean> selection) {
            }
        });

        //Listen if there are active gui jobs
        Jobs.MANAGER.getJobs().addListEventListener(listChanges -> {
            if (Jobs.MANAGER.hasActiveJobs()) {
                computationStarted();
            } else {
                computationCanceled();
            }
        });
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (isActive.get()) {
            Jobs.cancelALL();
        } else {
            new BatchComputeDialog(MF, MF.getCompounds());
        }
    }

    private void computationCanceled() {
        isActive.set(false);
        putValue(Action.NAME, "Compute All");
        putValue(Action.LARGE_ICON_KEY, Icons.RUN_32);
        putValue(Action.SMALL_ICON, Icons.RUN_16);
        putValue(Action.SHORT_DESCRIPTION, "Compute all compounds");
    }

    private void computationStarted() {
        isActive.set(true);
        putValue(Action.NAME, "Cancel All");
        putValue(Action.LARGE_ICON_KEY, Icons.CANCEL_32);
        putValue(Action.SMALL_ICON, Icons.CANCEL_16);
        putValue(Action.SHORT_DESCRIPTION, "Cancel all running computations");
    }

}
