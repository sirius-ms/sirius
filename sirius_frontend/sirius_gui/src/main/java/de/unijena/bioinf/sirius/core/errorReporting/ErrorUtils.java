package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;

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


}
