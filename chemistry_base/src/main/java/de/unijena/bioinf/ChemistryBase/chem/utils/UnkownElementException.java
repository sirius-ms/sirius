package de.unijena.bioinf.ChemistryBase.chem.utils;

public class UnkownElementException extends FormulaParsingException {
    public UnkownElementException() {
    }

    public UnkownElementException(String message) {
        super(message);
    }

    public UnkownElementException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnkownElementException(Throwable cause) {
        super(cause);
    }

    public UnkownElementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
