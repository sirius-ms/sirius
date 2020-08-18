package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.IOException;

public interface BayesianNetworkScoringStorage {

    public void storeScoring(MolecularFormula formula, BayesnetScoring scoring) throws IOException;

    public void storeDefaultScoring(BayesnetScoring scoring) throws IOException;
}
