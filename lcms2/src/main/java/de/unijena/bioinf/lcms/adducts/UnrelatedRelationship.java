package de.unijena.bioinf.lcms.adducts;

public class UnrelatedRelationship implements KnownMassDelta {

    @Override
    public boolean isCompatible(IonType left, IonType right) {
        return true;
    }
}
