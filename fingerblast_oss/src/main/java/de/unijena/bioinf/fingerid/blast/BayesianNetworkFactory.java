package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.IOException;

public interface BayesianNetworkFactory {

    public BayesnetScoring getScoringOrNull(MolecularFormula formula) throws IOException;

    public BayesnetScoring getScoringOrDefault(MolecularFormula formula) throws IOException;

    public BayesnetScoring getDefaultScoring() throws IOException;
}
