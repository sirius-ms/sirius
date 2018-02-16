package de.unijena.bioinf.sirius.core.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ErrorUtils {
    private static ErrorReportHandler REPORT_HANDLER;

    public static InputStream getErrorLoggingStream() throws Exception {
        if (REPORT_HANDLER == null) {
            for (Handler h : Logger.getLogger("").getHandlers()) {
                if (h instanceof ErrorReportHandler) {
                    REPORT_HANDLER = (ErrorReportHandler) h;
                    break;
                }
            }
        }
        if (REPORT_HANDLER != null) {
            byte[] arr = REPORT_HANDLER.flushToByteArray();
            if (arr != null)
                return new ByteArrayInputStream(arr);
            else
                throw new NullPointerException("Error logging Stream has no bytes");
        } else {
            throw new NullPointerException("No Error logging Stream available");
        }
    }
}
