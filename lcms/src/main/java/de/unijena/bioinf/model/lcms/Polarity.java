package de.unijena.bioinf.model.lcms;

public enum Polarity {
    POSITIVE(1), NEGATIVE(-1), UNKNOWN(0);

    public final int charge;

    Polarity(int charge) {
        this.charge = charge;
    }
}
