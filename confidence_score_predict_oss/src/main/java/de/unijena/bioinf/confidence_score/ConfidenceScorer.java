package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.ArrayList;
import java.util.Arrays;

public interface ConfidenceScorer {
    String NO_DISATANCE_ID = "NoDist";
    String DISATANCE_ID = "dist";

    String DB_ALL_ID = "All";
    String DB_BIO_ID = "Bio";

    String CE_NOTHING = "nothing";
    String CE_RAMP = "ramp";


    double computeConfidence(final Ms2Experiment exp, final IdentificationResult idResult, Scored<FingerprintCandidate>[] ranked_candidates_covscore, Scored<FingerprintCandidate>[] ranked_candidates_csiscore, Scored<FingerprintCandidate>[] ranked_candidates_covscore_filtered, Scored<FingerprintCandidate>[] ranked_candidates_csiscore_filtered, ProbabilityFingerprint query);
}