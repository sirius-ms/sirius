package de.unijena.bioinf.ChemistryBase.chem.utils;

public class FormulaParsingException extends Exception {
    public FormulaParsingException() {
    }

    public FormulaParsingException(String message) {
        super(message);
    }

    public FormulaParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormulaParsingException(Throwable cause) {
        super(cause);
    }

    public FormulaParsingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
