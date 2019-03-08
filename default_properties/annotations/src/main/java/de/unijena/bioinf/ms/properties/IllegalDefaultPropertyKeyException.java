package de.unijena.bioinf.ms.properties;

public class IllegalDefaultPropertyKeyException extends IllegalArgumentException {
    private static final String DEFAULT_MESSAGE = "Illegal key. The provided key is not a valid default property key! Not corresponding Class found!";

    public IllegalDefaultPropertyKeyException() {
        this(DEFAULT_MESSAGE);
    }

    public IllegalDefaultPropertyKeyException(String s) {
        super(s);
    }

    public IllegalDefaultPropertyKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalDefaultPropertyKeyException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }
}
