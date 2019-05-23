package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.sirius.IdentificationResult;

public interface ConfidenceScorer {
    String NO_DISATANCE_ID = "noDistance";
    String DISATANCE_ID = "distance";

//    String DB_ALL_ID = "";
//    String DB_BIO_ID = "";

    String CE_NOTHING = "nothing";
    String CE_RAMP = "ramp";
    double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] allCandidates, Scored<FingerprintCandidate>[] filteredCandidates, ProbabilityFingerprint query, final long filterFlag);
}