package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * Two ions might differ in a certain loss (e.g. H2O). In this case they need to have the same adduct,
 * but beyond that, are free in their choice of adduct.
 */
public class LossRelationship implements KnownMassDelta{

    protected MolecularFormula formula;
    protected boolean coversAdduct;

    private LossRelationship(MolecularFormula formula, boolean cov) {
        this.formula=formula;
        this.coversAdduct=cov;
    }

    public LossRelationship(MolecularFormula formula) {
        this(formula,false);
    }

    @Override
    public boolean isCompatible(IonType left, IonType right) {
        return left.ionType.getIonization().equals(right.ionType.getIonization()) && (int)(left.multimere)==(int)(right.multimere);
    }

    /**
     * @return true if the mass delta of this loss coincidences with the mass delta of two adducts
     */
    public boolean coversPotentialAdduct() {
        return coversAdduct;
    }

    public LossRelationship coveringAdduct() {
        return new LossRelationship(formula, true);
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
