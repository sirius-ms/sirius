package de.unijena.bioinf.sirius.logging;

import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextAreaJJobContainer<R> extends SwingJJobContainer<R> {
    private final JTextArea jobLog;

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName) {
        super(sourceJob, jobName);
        jobLog = connectToJobLogTextArea(this);
    }

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName, String jobCategory) {
        super(sourceJob, jobName, jobCategory);
        jobLog = connectToJobLogTextArea(this);
    }

    public JTextArea getJobLog() {
        return jobLog;
    }

    public static JTextArea connectToJobLogTextArea(SwingJJobContainer swingjob) {
        Logger logger = Logger.getLogger(swingjob.getSourceJob().LOG().getName());
        JTextArea logArea = new JTextArea();
        logger.addHandler(new TextAreaHandler(new TextAreaOutputStream(logArea), Level.INFO));

        return logArea;
    }
}
