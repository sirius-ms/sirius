package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * Two ions might differ in a certain loss (e.g. H2O). In this case they need to have the same adduct,
 * but beyond that, are free in their choice of adduct.
 */
public class LossRelationship implements KnownMassDelta{

    protected MolecularFormula formula;

    public LossRelationship(MolecularFormula formula) {
        this.formula = formula;
    }

    @Override
    public boolean isCompatible(KnownAdductType left, KnownAdductType right) {
        return left.equals(right);
    }

    @Override
    public String toString() {
        return "LossRelationship{" +
                "formula=" + formula +
                '}';
    }
}
