package de.unijena.bioinf.chemdb.custom;

import java.io.IOException;

public class OutdatedDBExeption extends IOException {
    public OutdatedDBExeption() {
        super();
    }

    public OutdatedDBExeption(String message) {
        super(message);
    }

    public OutdatedDBExeption(String message, Throwable cause) {
        super(message, cause);
    }

    public OutdatedDBExeption(Throwable cause) {
        super(cause);
    }
}
