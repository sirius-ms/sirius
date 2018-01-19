package de.unijena.bioinf.sirius.logging;

import de.unijena.bioinf.jjobs.SwingJJobContainer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class JobLogDialog extends JDialog {
    private final SwingJJobContainer jobContainer;
    private TextAreaHandler onDemandHandler;

    public JobLogDialog(Dialog owner, SwingJJobContainer source) {
        super(owner, "Job Log: " + source.getJobName() + " - " + source.getJobCategory());
        jobContainer = source;

        if (source instanceof TextAreaJJobContainer)
            add(new LoggingPanel(((TextAreaJJobContainer) source).getJobLog()));
        else {
            add(createOnDemandLoggingPanel());
        }

        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private JPanel createOnDemandLoggingPanel() {
        JTextArea area = new JTextArea();
        onDemandHandler = new TextAreaHandler(new TextAreaOutputStream(area), Level.INFO);
        Logger logger = LogManager.getLogManager().getLogger(jobContainer.getSourceJob().LOG().getName());
        logger.addHandler(onDemandHandler);

        return new LoggingPanel(area);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (onDemandHandler != null) {
            Logger logger = LogManager.getLogManager().getLogger(jobContainer.getSourceJob().LOG().getName());
            if (logger != null)
                logger.removeHandler(onDemandHandler);
        }
    }
}
