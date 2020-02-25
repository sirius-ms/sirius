package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface ConfidenceScorer {

    double computeConfidence(final Ms2Experiment exp, final IdentificationResult<?> idResult, Scored<FingerprintCandidate>[] allCandidates, ProbabilityFingerprint query, @NotNull final Predicate<FingerprintCandidate> filter);

    double computeConfidence(final Ms2Experiment exp, final IdentificationResult<?> idResult, Scored<FingerprintCandidate>[] pubchemCandidates, Scored<FingerprintCandidate>[] searchDBCandidates, ProbabilityFingerprint query);
}