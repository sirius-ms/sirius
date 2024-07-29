package de.unijena.bioinf.lcms.adducts;

public interface KnownMassDelta {
    public boolean isCompatible(IonType left, IonType right);
}
