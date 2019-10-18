package de.unijena.bioinf.ms.gui.logging;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;

import javax.swing.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

//this is a swing job wrapper that writes log output into a accociated text area instead into the console or the log file
public class TextAreaJJobContainer<R> extends SwingJJobContainer<R> {
    private final JTextArea jobLog;
    private final TextAreaHandler textAreaLogHandler;

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName) {
        super(sourceJob, jobName);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog(sourceJob);
    }

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName, String jobCategory) {
        super(sourceJob, jobName, jobCategory);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog(sourceJob);
    }

    public JTextArea getJobLog() {
        return jobLog;
    }

    private TextAreaHandler connectJobLogToTextArea() {
        return new TextAreaHandler(new TextAreaOutputStream(jobLog), Level.INFO);
    }

    public void registerJobLogs(JJob... jobs) {
        registerJobLogs(Arrays.asList(jobs));
    }

    public void registerJobLogs(Iterable<JJob> jobs) {
        for (JJob job : jobs) {
            registerJobLog(job);
        }
    }

    public void registerJobLog(JJob job) {
        Logger logger = Logger.getLogger(job.LOG().getName());
        logger.addHandler(textAreaLogHandler);
    }
}
