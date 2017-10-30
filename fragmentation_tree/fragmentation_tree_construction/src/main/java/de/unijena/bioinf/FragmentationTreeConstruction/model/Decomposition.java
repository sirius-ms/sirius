package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

public class Decomposition extends Scored<MolecularFormula> {

    protected final Ionization ion;

    public Decomposition(MolecularFormula candidate, Ionization ion, double score) {
        super(candidate, score);
        this.ion = ion;
    }

    public Ionization getIon() {
        return ion;
    }
}
