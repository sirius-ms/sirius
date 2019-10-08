package de.unijena.bioinf.ChemistryBase.exceptions;

public class InsufficientDataException extends Exception {
    private static final long serialVersionUID = 5389492791737363147L;

    public InsufficientDataException() {
    }

    public InsufficientDataException(String message) {
        super(message);
    }

    public InsufficientDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientDataException(Throwable cause) {
        super(cause);
    }

    public InsufficientDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
