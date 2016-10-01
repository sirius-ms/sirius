package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import de.unijena.bioinf.sirius.core.mailService.Mail;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ErrorReport {
    protected boolean sendSystemInfo = true;
    protected String subject = "";
    protected String userMessage = "";
    protected List<File> additionalFiles = Collections.emptyList();
    private String userEmail = null;
    private boolean sendReportToUser = true;

    public ErrorReport(String subject, String userMessage, List<File> additionalFiles) {
        this.subject = subject;
        this.userMessage = userMessage;
        this.additionalFiles = additionalFiles;
    }

    public ErrorReport(String subject) {
        setSubject(subject);
    }


    public boolean isSendSystemInfo() {
        return sendSystemInfo;
    }

    public void setSendSystemInfo(boolean sendSystemInfo) {
        this.sendSystemInfo = sendSystemInfo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public List<File> getAdditionalFiles() {
        return additionalFiles;
    }

    public void setAdditionalFiles(List<File> additionalFiles) {
        this.additionalFiles = additionalFiles;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public boolean setUserEmail(String userEmail) {
        if (Mail.validateMailAdress(userEmail)) {
            this.userEmail = userEmail;
            return true;
        } else if (userEmail == null || userEmail.isEmpty()) {
            this.userEmail = null;
            return true;
        }
        return false;
    }

    public boolean isSendReportToUser() {
        return sendReportToUser;
    }

    public void setSendReportToUser(boolean sendReportToUser) {
        this.sendReportToUser = sendReportToUser;
    }
}
