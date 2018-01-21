package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.*;

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

    /**
     * returns a new whiteset of all formulas that can be explained with the given mass and one
     * of the precursor ions
     */
    public List<Decomposition> resolve(double parentMass, Deviation deviation, Collection<PrecursorIonType> allowedPrecursorIonTypes) {
        final List<Decomposition> decompositions = new ArrayList<>();
        eachFormula:
        for (MolecularFormula formula : formulas) {
            for (PrecursorIonType ionType : allowedPrecursorIonTypes) {
                if (deviation.inErrorWindow(parentMass, ionType.neutralMassToPrecursorMass(formula.getMass()))) {
                    decompositions.add(new Decomposition(ionType.neutralMoleculeToMeasuredNeutralMolecule(formula), ionType.getIonization(), 0d));
                    continue eachFormula;
                }
            }
        }
        return decompositions;
    }

}
