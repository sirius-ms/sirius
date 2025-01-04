package de.unijena.bioinf.ms.persistence.storage.exceptions;

public class ProjectStateException extends IllegalStateException {
    public ProjectStateException() {
        super();
    }

    public ProjectStateException(String s) {
        super(s);
    }

    public ProjectStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectStateException(Throwable cause) {
        super(cause);
    }
}
