package de.unijena.bioinf.lcms.adducts;

public class UnrelatedRelationship implements KnownMassDelta {

    @Override
    public boolean isCompatible(KnownAdductType left, KnownAdductType right) {
        return true;
    }
}
