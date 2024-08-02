package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * Two ions might differ in a certain loss (e.g. H2O). In this case they need to have the same adduct,
 * but beyond that, are free in their choice of adduct.
 */
@Deprecated
public class MultimereRelationship implements KnownMassDelta{

    float multiplicator;

    public MultimereRelationship(float multiplicator) {
        this.multiplicator = multiplicator;
    }

    public float getMultiplicator() {
        return multiplicator;
    }

    @Override
    public boolean isCompatible(IonType left, IonType right) {
        if (!(left.ionType.equals(right.ionType) && left.insource.equals(right.insource)))
            return false;
        return (int)(left.multimere*multiplicator) == (int)right.multimere;
    }

    @Override
    public String toString() {
        return "multimereRelationship{ " + multiplicator + " }";
    }
}
