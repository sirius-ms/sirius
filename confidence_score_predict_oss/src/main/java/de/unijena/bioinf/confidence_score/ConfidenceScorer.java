package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.sirius.IdentificationResult;

public interface ConfidenceScorer {

    double computeConfidence(Ms2Experiment exp, Scored<FingerprintCandidate>[] allCandidates, Scored<FingerprintCandidate>[] filteredCandidates, ProbabilityFingerprint query, IdentificationResult idResult);


}