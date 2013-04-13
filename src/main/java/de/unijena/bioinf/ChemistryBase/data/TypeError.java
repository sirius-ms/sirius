package de.unijena.bioinf.ChemistryBase.data;


public class TypeError extends RuntimeException {

    public TypeError() {
    }

    public TypeError(String message) {
        super(message);
    }

    public TypeError(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeError(Throwable cause) {
        super(cause);
    }

    public TypeError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
