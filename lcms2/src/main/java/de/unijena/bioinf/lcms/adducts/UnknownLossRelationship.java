package de.unijena.bioinf.lcms.adducts;

public class UnknownLossRelationship implements KnownMassDelta {

    @Override
    public boolean isCompatible(IonType left, IonType right) {
        if (!(left.ionType.equals(right.ionType)))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "? -> ?";
    }
}
