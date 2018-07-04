package de.unijena.bioinf.chemdb;

import java.io.IOException;

public class DatabaseException extends IOException {
    public DatabaseException() {
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }
}
