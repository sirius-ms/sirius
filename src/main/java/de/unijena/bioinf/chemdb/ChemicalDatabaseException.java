package de.unijena.bioinf.chemdb;


import java.io.IOException;


public class ChemicalDatabaseException extends IOException {
    public ChemicalDatabaseException() {
    }

    public ChemicalDatabaseException(String message) {
        super(message);
    }

    public ChemicalDatabaseException(Throwable cause) {
        super(cause);
    }

    public ChemicalDatabaseException(String message, IOException e) {
        super(message, e);
    }
}
