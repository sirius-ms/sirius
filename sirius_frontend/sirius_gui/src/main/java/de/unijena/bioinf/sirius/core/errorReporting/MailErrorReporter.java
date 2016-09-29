package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import de.unijena.bioinf.babelms.utils.Compress;
import de.unijena.bioinf.sirius.core.mailService.Mail;
import de.unijena.bioinf.sirius.core.systemInfo.SystemInformation;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MailErrorReporter extends ErrorReporter {

    public MailErrorReporter() {
    }

    public MailErrorReporter(String subject, String userMessage, String userEmail) {
        super(subject, userMessage, userEmail);
    }

    //todo progress report?
    @Override
    protected int reportError(ErrorReport report) {
        int rValue = 0;
        EmailAttachment errorReportAttachment = null;

        try (PipedOutputStream sysInfoOut = new PipedOutputStream()) {
            Map<InputStream, String> is = new HashMap<>();

            //create system info
            if (report.isSendSystemInfo()) {
                LoggerFactory.getLogger(ErrorUtils.class).info("Collection system Information");
                SystemInformation.writeSystemInformationTo(sysInfoOut);
                PipedInputStream sysInfoIn = new PipedInputStream(sysInfoOut);
                is.put(sysInfoIn, "system.info");
            }

            //collect attachments for compression
            LoggerFactory.getLogger(ErrorUtils.class).info("Collection log files");
            for (File f : report.getAdditionalFiles()) {
                is.put(new FileInputStream(f), f.getName());
            }


            LoggerFactory.getLogger(ErrorUtils.class).info("Compressing data log files");
            File errorReportFile = File.createTempFile("ErrorReportLog", ".zip");
            Compress.compressToZipArchive(errorReportFile, is);

            //todo set user email as sender if given

            LoggerFactory.getLogger(ErrorUtils.class).info("Building attachment");
            errorReportAttachment = new EmailAttachment();
            errorReportAttachment.setPath(errorReportFile.getAbsolutePath());
            errorReportAttachment.setDisposition(EmailAttachment.ATTACHMENT);
            errorReportAttachment.setDescription("Sirius error information File");
            errorReportAttachment.setName(errorReportFile.getName());

        } catch (IOException e) {
            LoggerFactory.getLogger(ErrorUtils.class).error("Could not create ErrorReport Archive", e);
            rValue = 2;
        }


        try {
            LoggerFactory.getLogger(ErrorUtils.class).info("Creating email");
            MultiPartEmail email = new MultiPartEmail();
            email.setFrom(Mail.getSender(), "Sirius Error Reporter");
            email.setSubject("Sirius Error: " + report.getSubject());

            email.setMsg(
                    "Contact:" + report.getUserEmail() +
                            System.lineSeparator() +
                            System.lineSeparator() +
                            report.getUserMessage()
            );
//            email.addTo("Sirius-devel@listserv.uni-jena.de");
            email.addTo("markus.fleischauer@gmail.com");

            // add the attachment
            if (errorReportAttachment != null)
                email.attach(errorReportAttachment);

            LoggerFactory.getLogger(ErrorUtils.class).info("Sending error report");
            Mail.send(email);
            LoggerFactory.getLogger(ErrorUtils.class).info("Error Report Successful sent!");
        } catch (EmailException e) {
            LoggerFactory.getLogger(ErrorUtils.class).error("Could not send Error report!", e);
            rValue = 1;
        }
        return rValue = 0;
    }


}
