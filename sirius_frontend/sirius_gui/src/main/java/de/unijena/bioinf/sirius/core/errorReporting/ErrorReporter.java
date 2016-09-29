package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ErrorReporter extends SwingWorker<Integer, String> {
    private ErrorReport report = null;


    public ErrorReporter() {
    }

    public ErrorReporter(String subject, String userMessage, String userEmail) {
        setDefaultReport(subject, userMessage, userEmail);
    }

    public void setReport(ErrorReport report) {
        this.report = report;
    }

    public void setDefaultReport(String subject, String userMessage, String userEmail) {
        this.report = new SiriusDefaultErrorReport(subject, userMessage, userEmail);
    }


    abstract int reportError(ErrorReport report);

    @Override
    protected Integer doInBackground() throws Exception {
        if (report == null) {
            LoggerFactory.getLogger(this.getClass()).warn("Nothing to report! Process finished");
            return 3;
        }
        return reportError(report);
    }

    public static MailErrorReporter newMailErrorReporter(String subject, String userMessage, String userEmail) {
        return new MailErrorReporter(subject, userMessage, userEmail);
    }
}
