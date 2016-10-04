package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 04.10.16.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class WebErrorReporter extends ErrorReporter {

    public WebErrorReporter() {
    }

    public WebErrorReporter(String subject, String userMessage, String userMail) {
        super(subject, userMessage, userMail);
    }

    @Override
    int reportError(ErrorReport report) {
        //todo implement it
        return 0;
    }
}
