package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

public interface Reaction {
    boolean hasReaction(MolecularFormula var1, MolecularFormula var2);

    default boolean hasReactionAnyDirection(MolecularFormula mf1, MolecularFormula mf2) {
        return this.hasReaction(mf1, mf2) || this.hasReaction(mf2, mf1);
    }

    MolecularFormula netChange();

    Reaction negate();

    int stepSize();
}
