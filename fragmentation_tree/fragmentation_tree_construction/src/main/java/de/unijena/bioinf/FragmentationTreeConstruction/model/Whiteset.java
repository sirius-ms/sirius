package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Set;

/**
 * Only allow certain molecular formulas as root
 */
public class Whiteset {

    protected final Set<MolecularFormula> formulas;

    public Whiteset(Set<MolecularFormula> formulas) {
        this.formulas = formulas;
    }

    public Set<MolecularFormula> getFormulas() {
        return formulas;
    }
}
