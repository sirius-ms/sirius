package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;

import java.io.File;
import java.util.ArrayList;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusDefaultErrorReport extends ErrorReport {
    public SiriusDefaultErrorReport(String subject, String userMessage) {
        super(subject);
        setUserMessage(userMessage);
        additionalFiles = new ArrayList<>(3);

        additionalFiles.add(ApplicationCore.WORKSPACE.resolve("sirius.properties").toFile());
        additionalFiles.add(ApplicationCore.WORKSPACE.resolve("logging.properties").toFile());
        additionalFiles.add(new File(ErrorUtils.getCurrentLogFile()));
    }
}
