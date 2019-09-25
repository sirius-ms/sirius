package de.unijena.bioinf.ChemistryBase.chem.utils;

public class UnknownElementException extends FormulaParsingException {
    public UnknownElementException() {
    }

    public UnknownElementException(String message) {
        super(message);
    }

    public UnknownElementException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownElementException(Throwable cause) {
        super(cause);
    }

    public UnknownElementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
