package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

public interface FingerblastScoringMFSpecific extends FingerblastScoring {

    //todo adjust so all scoring only use the new method
    void prepare(ProbabilityFingerprint fingerprint, MolecularFormula formula);

}
