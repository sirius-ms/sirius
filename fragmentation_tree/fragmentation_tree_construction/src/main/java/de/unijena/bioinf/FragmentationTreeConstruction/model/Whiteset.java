package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Only allow certain molecular formulas as root
 */
public class Whiteset {

    public static Whiteset of(MolecularFormula... formulas) {
        return new Whiteset(new HashSet<MolecularFormula>(Arrays.asList(formulas)));
    }

    protected final Set<MolecularFormula> formulas;

    public Whiteset(Set<MolecularFormula> formulas) {
        this.formulas = formulas;
    }

    public Set<MolecularFormula> getFormulas() {
        return formulas;
    }
}
