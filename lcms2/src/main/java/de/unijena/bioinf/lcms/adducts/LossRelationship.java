package de.unijena.bioinf.lcms.adducts;

/**
 * Two ions might differ in a certain loss (e.g. H2O). In this case they need to have the same adduct,
 * but beyond that, are free in their choice of adduct.
 */
public class LossRelationship implements KnownMassDelta{


    @Override
    public boolean isCompatible(KnownAdductType left, KnownAdductType right) {
        return left.equals(right);
    }
}
