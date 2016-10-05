package de.unijena.bioinf.sirius.core.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusDefaultErrorReport extends ErrorReport {

    public SiriusDefaultErrorReport(String subject, String userMessage, String userEmail, boolean sendSystemInfo) {
        super(subject);
        setUserMessage(userMessage);
        setUserEmail(userEmail);

        File f = null;
        try {
            f = ApplicationCore.WORKSPACE.resolve("sirius.properties").toFile();
            addAdditionalFiles(f);
            f = ApplicationCore.WORKSPACE.resolve("logging.properties").toFile();
            addAdditionalFiles(f);
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(this.getClass()).error("Could not load file: " + f.getAbsolutePath(), e);
        }
        try {
            addAdditionalFiles(ErrorUtils.getErrorLoggingStream(), "sirius.log");
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Could not load Logging Stream", e);
        }

        //create system info
        if (sendSystemInfo) {
            try {
                addSystemInfoFile();
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("Could not create System info file", e);
            }
        }
    }

    //this constructor is just for deserialisation
    public SiriusDefaultErrorReport(){
        super();

    }
}


