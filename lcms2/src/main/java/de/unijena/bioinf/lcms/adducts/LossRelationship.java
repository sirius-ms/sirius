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
    public boolean isCompatible(IonType left, IonType right) {
        if (!(left.ionType.equals(right.ionType) && (int)left.multimere==(int)right.multimere))
            return false;
        return left.insource.subtract(right.insource).equals(formula);
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    @Override
    public String toString() {
        if (formula.isAllPositiveOrZero()) {
            return "gain of " + formula;
        } else {
            return "loss of " + formula.negate();
        }
    }
}
