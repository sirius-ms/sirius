package de.unijena.bioinf.ms.rest.model.canopus;

public class CanopusJobData {
    //optional fields
    public final String securityToken;
    public final byte[] compoundClasses; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES

    private CanopusJobData() {
        this(null, null);
    }

    public CanopusJobData(String securityToken, byte[] compoundClasses) {
        this.securityToken = securityToken;
        this.compoundClasses = compoundClasses;
    }
}
