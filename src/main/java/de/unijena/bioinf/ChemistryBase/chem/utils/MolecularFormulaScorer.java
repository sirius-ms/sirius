package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * Scores a molecular formula using a prior
 */
public interface MolecularFormulaScorer {

    public double score(MolecularFormula formula);

}
