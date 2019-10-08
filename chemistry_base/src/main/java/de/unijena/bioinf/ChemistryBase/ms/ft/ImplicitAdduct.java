package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

/**
 * This annotation is set whenever we remove an adduct from the tree (see IonTreeUtils).
 * If a fragment looses an adduct, we neutralize the molecular formula, but add the ImplicitAdduct annotation.
 * If a loss looses an adduct, the adduct formula is subtracted from the loss formula and the ImplicitAdduct annotation
 * is set. However, the subtree is then already neutralized and does not get an ImplicitAdduct annotation.
 */
public class ImplicitAdduct implements TreeAnnotation  {

    protected final MolecularFormula adductFormula;

    private static final ImplicitAdduct NO_ADDUCT = new ImplicitAdduct(MolecularFormula.emptyFormula());

    public static ImplicitAdduct none() {
        return NO_ADDUCT;
    }

    public ImplicitAdduct() {
        this(MolecularFormula.emptyFormula());
    }

    public ImplicitAdduct(MolecularFormula adductFormula) {
        this.adductFormula = adductFormula;
    }

    public MolecularFormula getAdductFormula() {
        return adductFormula;
    }

    public boolean hasImplicitAdduct() {
        return !adductFormula.isEmpty();
    }
}
