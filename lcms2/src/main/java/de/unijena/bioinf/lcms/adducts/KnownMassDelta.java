package de.unijena.bioinf.lcms.adducts;

public interface KnownMassDelta {
    public boolean isCompatible(KnownAdductType left, KnownAdductType right);
}
