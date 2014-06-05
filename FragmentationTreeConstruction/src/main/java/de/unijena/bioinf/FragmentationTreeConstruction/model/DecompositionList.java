package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;

import java.util.ArrayList;
import java.util.List;

public class DecompositionList {

    private final List<ScoredMolecularFormula> decompositions;

    public static DecompositionList fromFormulas(List<MolecularFormula> formulas) {
        final ArrayList<ScoredMolecularFormula> decompositions = new ArrayList<ScoredMolecularFormula>(formulas.size());
        for (MolecularFormula f : formulas) decompositions.add(new ScoredMolecularFormula(f,0d));
        return new DecompositionList(decompositions);

    }

    public DecompositionList(List<ScoredMolecularFormula> decompositions) {
        this.decompositions = decompositions;
    }

    public List<ScoredMolecularFormula> getDecompositions() {
        return decompositions;
    }
}
