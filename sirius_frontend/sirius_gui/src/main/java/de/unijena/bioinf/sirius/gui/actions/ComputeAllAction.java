package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.sirius.gui.compute.JobLog;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.configs.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

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
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {
                setEnabled(event.getSourceList().size() > 0);
            }
            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {}
        });

        JobLog.getInstance().addListener(new JobLog.JobListener() {
            @Override
            public void jobIsSubmitted(JobLog.Job job) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (JobLog.getInstance().hasActiveJobs()) {
                            computationStarted();
                        } else {
                            computationCanceled();
                        }
                    }
                });
            }

            @Override
            public void jobIsRunning(JobLog.Job job) {
                jobIsSubmitted(job);
            }

            @Override
            public void jobIsDone(final JobLog.Job job) {
                jobIsSubmitted(job);
            }

            @Override
            public void jobIsFailed(JobLog.Job job) {
                jobIsSubmitted(job);
            }

            @Override
            public void jobDescriptionChanged(JobLog.Job job) {
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isActive.get()) {
            MF.getBackgroundComputation().cancelAll();
        } else {
            new BatchComputeDialog(MF, MF.getCompounds());
        }
    }

    private void computationCanceled() {
        isActive.set(false);
        putValue(Action.NAME, "Compute All");
        putValue(Action.LARGE_ICON_KEY, Icons.RUN_32);
        putValue(Action.SMALL_ICON, Icons.RUN_16);
        putValue(Action.SHORT_DESCRIPTION, "Compute all Experiments");
    }

    private void computationStarted() {
        isActive.set(true);
        putValue(Action.NAME, "Cancel All");
        putValue(Action.LARGE_ICON_KEY, Icons.CANCEL_32);
        putValue(Action.SMALL_ICON, Icons.CANCEL_16);
        putValue(Action.SHORT_DESCRIPTION, "Cancel all running computations");
    }

}
