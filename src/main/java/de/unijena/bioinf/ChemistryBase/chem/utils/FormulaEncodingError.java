package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

public class FormulaEncodingError extends RuntimeException {

    public FormulaEncodingError(MolecularFormula formula) {
        this("can't encode " + formula);
    }

    public FormulaEncodingError(long a) {
        this("can't decode " + a);
    }

    public FormulaEncodingError() {
    }

    public FormulaEncodingError(String message) {
        super(message);
    }

    public FormulaEncodingError(String message, Throwable cause) {
        super(message, cause);
    }

    public FormulaEncodingError(Throwable cause) {
        super(cause);
    }

    public FormulaEncodingError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
