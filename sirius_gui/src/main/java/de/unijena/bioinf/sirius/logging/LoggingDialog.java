package de.unijena.bioinf.sirius.logging;

import de.unijena.bioinf.jjobs.SwingJJobContainer;

import javax.swing.*;
import java.awt.*;

public class LoggingDialog extends JDialog {
    public LoggingDialog(Dialog owner, SwingJJobContainer source) {
        super(owner, "Job Log");
        if (source instanceof TextAreaJJobContainer)
            add(new LoggingPanel(((TextAreaJJobContainer) source).getJobLog()));
        //todo add handler for live logging of non textarea jobs
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }
}
