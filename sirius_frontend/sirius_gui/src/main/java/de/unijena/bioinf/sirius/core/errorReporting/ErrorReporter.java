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
    public final static String REPORT_ADRESS = "markus.fleischauer@gmail.com";
    private ErrorReport report = null;


    public ErrorReporter() {
    }

    public ErrorReporter(String subject, String userMessage, String userMail) {
        setDefaultReport(subject, userMessage, userMail);
    }

    public void setReport(ErrorReport report) {
        this.report = report;
    }

    public ErrorReport getReport() {
        return report;
    }

    public boolean setDefaultReport(String subject, String userMessage, String userMail) {
        this.report = new SiriusDefaultErrorReport(subject, userMessage);
        return this.report.setUserEmail(userMail);
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

    public static MailErrorReporter newMailErrorReporter(String subject, String userMessage, String userMail) {
        return new MailErrorReporter(subject, userMessage, userMail);
    }
}
