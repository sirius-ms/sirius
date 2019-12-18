package de.unijena.bioinf.ms.rest.model.canopus;

/**
 * Class containing the input for Canopus Jobs
 * Will be (De-)Marshaled to/from json
 */
public class CanopusJobInput {
    protected String formula;
    public final byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES

    private CanopusJobInput() {
        this(null,null);
    }

    public CanopusJobInput(String formula, byte[] fingerprint) {
        this.fingerprint = fingerprint;
        this.formula = formula;
    }
}
