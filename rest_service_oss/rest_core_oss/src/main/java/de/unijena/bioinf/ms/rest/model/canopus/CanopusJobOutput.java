package de.unijena.bioinf.ms.rest.model.canopus;

/**
 * Output data of a Canopus Job see {@link de.unijena.bioinf.ms.rest.model.JobTable}
 */
public class CanopusJobOutput {
    public final byte[] compoundClasses; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES

    private CanopusJobOutput() {
        this(null);
    }

    public CanopusJobOutput(byte[] compoundClasses) {
        this.compoundClasses = compoundClasses;
    }
}
