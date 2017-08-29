package de.unijena.bioinf.GibbsSampling.model;

/**
 * Created by ge28quv on 01/06/17.
 */
public class GraphValidationMessage {

    private final String message;
    private final boolean isError;
    private final boolean isWarning;

    public GraphValidationMessage(String message, boolean isError, boolean isWarning) {
        this.message = message;
        this.isError = isError;

        if (isError) this.isWarning = false;
        else {
            this.isWarning = isWarning;
        }
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return isError;
    }

    public boolean isWarning() {
        return isWarning;
    }
}
