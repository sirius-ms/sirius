package de.unijena.bioinf.ms.rest.model.canopus;

/**
 * Class containing the input for Canopus Jobs
 * Will be (De-)Marshaled to/from json
 */
public class CanopusJobInput {
    public final byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES

    private CanopusJobInput() {
        this(null);
    }

    public CanopusJobInput(byte[] fingerprint) {
        this.fingerprint = fingerprint;
    }
}
