package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.sirius.gui.compute.JobLog;
import de.unijena.bioinf.sirius.gui.utils.Icons;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ShowJobsDialogAction extends AbstractAction {

    public ShowJobsDialogAction() {
        super("Jobs");
        putValue(Action.LARGE_ICON_KEY, Icons.FB_LOADER_STOP_32);
        putValue(Action.SHORT_DESCRIPTION, "Show background jobs and their status");


        JobLog.getInstance().addListener(new JobLog.JobListener() {
            @Override
            public void jobIsSubmitted(JobLog.Job job) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (JobLog.getInstance().hasActiveJobs()) {
                            putValue(Action.LARGE_ICON_KEY, Icons.FB_LOADER_RUN_32);
                        } else {
                            putValue(Action.LARGE_ICON_KEY, Icons.FB_LOADER_STOP_32);
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
        MF.getJobDialog().showDialog();
    }
}
