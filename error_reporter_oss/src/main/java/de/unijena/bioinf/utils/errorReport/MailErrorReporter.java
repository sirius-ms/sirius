/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.utils.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import de.unijena.bioinf.utils.mailService.Mail;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.slf4j.LoggerFactory;

import javax.mail.util.ByteArrayDataSource;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class MailErrorReporter extends ErrorReporter {
    public final String REPORT_ADRESS;


    public MailErrorReporter(String REPORT_ADRESS, String SOFTWARE_NAME, ErrorReport report) {
        super(SOFTWARE_NAME, report);
        this.REPORT_ADRESS = REPORT_ADRESS;
    }

    public MailErrorReporter(String SOFTWARE_NAME, ErrorReport report) {
        super(SOFTWARE_NAME, report);
        REPORT_ADRESS = initReportAdress();
    }


    private String initReportAdress() {
        String mail = ErrorReporter.properties.getProperty("de.unijena.bioinf.utils.errorReport.mailTo");
        return Mail.validateMailAdress(mail) ? mail : null;
    };

    @Override
    protected String reportError(ErrorReport report) throws EmailException {
        int rValue = 0;
        ByteArrayDataSource attachment = new ByteArrayDataSource(report.getAdditionalFilesAsCompressedBytes(), "application/zip");
        String desc = SOFTWARE_NAME + " Error information file";
        String name = "ErrorReport-" + report.getIdentifier() + ".zip";

        LoggerFactory.getLogger(this.getClass()).info("Creating email");
        MultiPartEmail reportMail = createMail(report);

        String message = report.getUserMessage();
        if (!report.getUserEmail().equals(report.NO_USER_MAIL)) {
            //send a copy of the report to user
            if (report.isSendReportToUser()) {

                String userMessage = "This is an auto generated mail sent by the " + SOFTWARE_NAME + " sofware containing the error report you have sent to the " + SOFTWARE_NAME + " developers" +
                        System.lineSeparator() + "Please do not reply to this email." +
                        System.lineSeparator() + System.lineSeparator() + "Your message was: " + System.lineSeparator() + report.getUserMessage();

                MultiPartEmail userMail = createMail(report);
                userMail.setMsg(report.getHeadline() + System.lineSeparator() + System.lineSeparator() + userMessage);

                userMail.addReplyTo("noReply@uni-jena.de", SOFTWARE_NAME + " Error Reporter");
                // add the attachment
                if (attachment != null)
                    userMail.attach(attachment, name, desc);

                userMail.addTo(report.getUserEmail());
                LoggerFactory.getLogger(this.getClass()).info("Sending error report to User: " + report.getUserEmail());
                Mail.send(userMail);
            }

            reportMail.addReplyTo(report.getUserEmail(), SOFTWARE_NAME + " Error Reporter");
            message = "Sender Contact: " + report.getUserEmail() + System.lineSeparator() + System.lineSeparator() + message;
        } else {
            reportMail.addReplyTo("noReply@uni-jena.de", SOFTWARE_NAME + " Error Reporter");
        }
        reportMail.setMsg(report.getHeadline() + System.lineSeparator() + System.lineSeparator() + message);
        reportMail.addTo(REPORT_ADRESS);
        if (attachment != null)
            reportMail.attach(attachment, name, desc);


        LoggerFactory.getLogger(this.getClass()).info("Sending error report to: " + REPORT_ADRESS);
        Mail.send(reportMail);

        LoggerFactory.getLogger(this.getClass()).info("Error Report Successful sent!");

        return "SUCCESS";
    }

    private MultiPartEmail createMail(ErrorReport report) throws EmailException {
        MultiPartEmail email = new MultiPartEmail();

        email.setFrom(Mail.getSender());
        email.setSubject(SOFTWARE_NAME + "-" + report.getVersion() + " "+ report.getType() + " [" + report.getIdentifier() + "]: " + report.getSubject());

        return email;

    }


}
