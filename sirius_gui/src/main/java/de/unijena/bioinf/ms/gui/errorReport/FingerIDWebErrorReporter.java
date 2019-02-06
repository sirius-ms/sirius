package de.unijena.bioinf.ms.gui.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 04.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.utils.errorReport.ErrorReporter;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FingerIDWebErrorReporter extends ErrorReporter {

    public FingerIDWebErrorReporter() {
    }

    public FingerIDWebErrorReporter(String SOFTWARE_NAME, ErrorReport report) {
        super(SOFTWARE_NAME, report);
    }

    public FingerIDWebErrorReporter(ErrorReport report) {
        super(report);
    }

    @Override
    protected String reportError(ErrorReport report) throws IOException, URISyntaxException {
        String r = ApplicationCore.WEB_API.reportError(report, SOFTWARE_NAME);
        return r;
    }
}
