package de.unijena.bioinf.ms.rest.model.canopus;

import de.unijena.bioinf.fingerid.predictor_types.PredictorType;

import java.util.EnumSet;

/**
 * Class containing the input for Canopus Jobs
 * Will be (De-)Marshaled to/from json
 */
public class CanopusJobInput {
    protected final String formula;
    public final byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    public final PredictorType predictor;

    private CanopusJobInput() {
        this(null,null, null);
    }

    public CanopusJobInput(String formula, byte[] fingerprint, PredictorType predictor) {
        this.fingerprint = fingerprint;
        this.formula = formula;
        this.predictor = predictor;
    }
}
