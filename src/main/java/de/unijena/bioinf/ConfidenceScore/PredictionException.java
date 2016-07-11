package de.unijena.bioinf.ConfidenceScore;

/**
 * Created by Marcus Ludwig on 11.07.16.
 */
public class PredictionException extends Exception {

    public PredictionException() {
    }

    public PredictionException(String message) {
        super(message);
    }

    public PredictionException(Throwable cause) {
        super(cause);
    }
}
