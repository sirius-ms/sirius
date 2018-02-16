package de.unijena.bioinf.ChemistryBase.jobs;

import de.unijena.bioinf.jjobs.*;
import org.junit.Test;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

public class SiriusJobsTest {

    @Test
    public void testRandom() throws InterruptedException {
        SiriusJobs.setGlobalJobManager(new SwingJobManager(4));
        SwingJobManager manager = (SwingJobManager) SiriusJobs.getGlobalJobManager();
        JJobTable table = new JJobTable(manager);
        JJobManagerPanel jobPanel = new JJobManagerPanel(table);


        JFrame frame = new JFrame("JFrame Example");
        frame.add(jobPanel);
        frame.setSize(300, 300);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        int num = 10;

        for (int i = 0; i < num; i++) {
            manager.submitSwingJob(new SwingJJobContainer(new RandomSwingJob(JJob.JobType.CPU), "job-" + i));
        }

        for (int i = 0; i < num; i++) {
            SwingJJobContainer j = new SwingJJobContainer(new RandomSwingJob(JJob.JobType.CPU), "job-" + i + "PRIO");
            j.getSourceJob().setPriority(JJob.JobPriority.HIGH);
            manager.submitSwingJob(j);

        }

        TimeUnit.SECONDS.sleep(5);

        for (int i = 0; i < num; i++) {
            manager.submitSwingJob(new SwingJJobContainer(new RandomSwingJob(JJob.JobType.IO), "job-" + i));
        }

        TimeUnit.SECONDS.sleep(5);

        for (int i = 0; i < num; i++) {
            manager.submitSwingJob(new SwingJJobContainer(new RandomSwingJob(JJob.JobType.REMOTE), "job-" + i));
            manager.submitSwingJob(new SwingJJobContainer(new RandomSwingJob(JJob.JobType.WEBSERVICE), "job-" + i + "b"));
        }
    }
}
