package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusDefaultErrorReport extends ErrorReport {
    public SiriusDefaultErrorReport(String subject, String userMessage) {
        super(subject);
        setUserMessage(userMessage);
        additionalFiles = new HashMap<>(3);

        File f = null;
        try {
            f = ApplicationCore.WORKSPACE.resolve("sirius.properties").toFile();
            additionalFiles.put(new FileInputStream(f), f.getName());
            f = ApplicationCore.WORKSPACE.resolve("logging.properties").toFile();
            additionalFiles.put(new FileInputStream(f), f.getName());
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(this.getClass()).error("Could not load file: " + f.getAbsolutePath(), e);
        }
        try {
            additionalFiles.put(ErrorUtils.getErrorLoggingStream(), "sirius.log");
//            additionalFiles.put(ErrorUtils.getFileLoggingStream(), "sirius.log");
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Could not load Logging Stream", e);
        }
    }
}
