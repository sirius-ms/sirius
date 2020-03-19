package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public interface ConfidenceScorer {

    double computeConfidence(@NotNull final Ms2Experiment exp, @NotNull final IdentificationResult<?> idResult, @NotNull List<Scored<FingerprintCandidate>> allDbCandidates, @NotNull ProbabilityFingerprint query, @Nullable final Predicate<FingerprintCandidate> filter);

    double computeConfidence(@NotNull final Ms2Experiment exp, @NotNull final IdentificationResult<?> idResult, @NotNull List<Scored<FingerprintCandidate>> allDbCandidates, @NotNull List<Scored<FingerprintCandidate>> searchDBCandidates, @NotNull ProbabilityFingerprint query);
}