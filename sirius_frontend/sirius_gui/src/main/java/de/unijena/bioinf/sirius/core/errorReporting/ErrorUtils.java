package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import java.io.*;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ErrorUtils {
    private static ErrorReportHandler REPORT_HANDLER;

    public static InputStream getErrorLoggingStream() throws IOException {
        if (REPORT_HANDLER ==  null){
            for (Handler h : Logger.getLogger("").getHandlers()) {
                if (h instanceof ErrorReportHandler){
                    REPORT_HANDLER = (ErrorReportHandler) h;
                }
            }
        }
        ByteArrayInputStream in =  new ByteArrayInputStream(REPORT_HANDLER.flushToByteArray());
        return in;
    }
}
