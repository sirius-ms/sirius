package de.unijena.bioinf.lcms.adducts;

public class UnknownLossRelationship implements KnownMassDelta {

    @Override
    public boolean isCompatible(IonType left, IonType right) {
        if (!(left.ionType.equals(right.ionType) && (int)left.multimere==(int)right.multimere))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "? -> ?";
    }
}
