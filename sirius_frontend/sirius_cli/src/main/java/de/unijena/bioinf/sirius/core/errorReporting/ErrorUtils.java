package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.slf4j.LoggerFactory;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ErrorUtils {

    private ErrorUtils() {
    }


    private static String searchLogFile() {
        return ApplicationCore.WORKSPACE.resolve("sirius.log.0").toString();
    }

    public static String getCurrentLogFile() {
        return searchLogFile();
    } //todo search for real log file

    public static void sendErrorReport(String errorMessage, String userMessage) {
        //todo error handling for files
        EmailAttachment logFile = new EmailAttachment();
        logFile.setPath(getCurrentLogFile());
        logFile.setDisposition(EmailAttachment.ATTACHMENT);
        logFile.setDescription("Sirius Log File");
        logFile.setName("sirius.log");

        EmailAttachment propFile = new EmailAttachment();
        propFile.setPath(ApplicationCore.WORKSPACE.resolve("sirius.properties").toString());
        propFile.setDisposition(EmailAttachment.ATTACHMENT);
        propFile.setDescription("Sirius Properties File");
        propFile.setName("sirius.properties");

        EmailAttachment logpropFile = new EmailAttachment();
        logpropFile.setPath(ApplicationCore.WORKSPACE.resolve("logging.properties").toString());
        logpropFile.setDisposition(EmailAttachment.ATTACHMENT);
        logpropFile.setDescription("Sirius logging Properties File");
        logpropFile.setName("logging.properties");

        EmailAttachment sysInfoFile = new EmailAttachment();
        sysInfoFile.setPath(SystemInformation.getTMPSystemInformationFile().getAbsolutePath());
        sysInfoFile.setDisposition(EmailAttachment.ATTACHMENT);
        sysInfoFile.setDescription("Sirius system information File");
        sysInfoFile.setName("system.info");


        MultiPartEmail email = SendMail.getEmailInstance();
        try {
            email.setFrom("finleymcserver@gmail.com","Sirius Error Reporter");
            email.setSubject("Sirius Error: " + errorMessage);
            email.setMsg(
                    "This is a test mail ... :-)" + System.lineSeparator() + userMessage
            );
            email.addTo("markus.fleischauer@gmail.com");

            // add the attachment
//            email.attach(logFile);
            email.attach(propFile);
            email.attach(logpropFile);
            email.attach(sysInfoFile);

            email.send();
            LoggerFactory.getLogger(ErrorUtils.class).info("Error Report Successful sent!");
        } catch (EmailException e) {
            LoggerFactory.getLogger(ErrorUtils.class).error("Could not send Error report!",e);
        }

    }
}
