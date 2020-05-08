package de.unijena.bioinf.ms.rest.model.fingerid;

public class FingerprintJobOutput {
    //optional fields
    public final byte[] fingerprint; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    public final byte[] iokrVector; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES


    private FingerprintJobOutput() {
        this(null, null);
    }

    public FingerprintJobOutput(byte[] fingerprint, byte[] iokrVector) {
        this.fingerprint = fingerprint;
        this.iokrVector = iokrVector;
    }
}

