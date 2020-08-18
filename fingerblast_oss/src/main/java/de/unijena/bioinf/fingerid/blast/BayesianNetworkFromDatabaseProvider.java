package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.IOException;

public class BayesianNetworkFromDatabaseProvider implements BayesianNetworkScoringProvider {

    @Override
    public BayesnetScoring getScoringOrNull(MolecularFormula formula) throws IOException {
        return null;
    }

    @Override
    public BayesnetScoring getScoringOrDefault(MolecularFormula formula) throws IOException {
        return null;
    }

    @Override
    public void storeScoring(MolecularFormula formula, BayesnetScoring scoring) {

    }

    @Override
    public BayesnetScoring getDefaultScoring() throws IOException {
        return null;
    }

    @Override
    public void storeDefaultScoring(BayesnetScoring scoring) throws IOException {

    }
}
