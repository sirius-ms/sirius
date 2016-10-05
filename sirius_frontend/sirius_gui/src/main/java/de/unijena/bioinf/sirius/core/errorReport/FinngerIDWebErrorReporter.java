package de.unijena.bioinf.sirius.core.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 04.10.16.
 */

import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.utils.errorReport.ErrorReporter;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FinngerIDWebErrorReporter extends ErrorReporter {

    public FinngerIDWebErrorReporter() {
    }

    public FinngerIDWebErrorReporter(String SOFTWARE_NAME, ErrorReport report) {
        super(SOFTWARE_NAME, report);
    }

    public FinngerIDWebErrorReporter(ErrorReport report) {
        super(report);
    }

    @Override
    protected String reportError(ErrorReport report) throws IOException, URISyntaxException {
        WebAPI csi = new WebAPI();
        return csi.reportError(report, SOFTWARE_NAME);
    }
}
