package de.unijena.bioinf.model.lcms;

public enum Polarity {
    POSITIVE(1), NEGATIVE(-1), UNKNOWN(0);

    public final int charge;

    Polarity(int charge) {
        this.charge = charge;
    }

    public static Polarity fromCharge(int charge) {
        switch (charge) {
            case 1:
                return POSITIVE;
            case -1:
                return NEGATIVE;
            case 0:
                return UNKNOWN;
            default:
                throw new IllegalArgumentException("Multi charges are not Supported!");
        }
    }
}
