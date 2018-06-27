package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public interface FeatureCreator extends Parameterized {

    void prepare(PredictionPerformance[] statistics);

    /**
     *
     * @param query
     * @param rankedCandidates sorted best to worst hit!
     * @return
     */
    double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, Scored<FingerprintCandidate>[] rankedCandidates, IdentificationResult idresult, long flags);

    int getFeatureSize();

    boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates);

    int getRequiredCandidateSize();

    String[] getFeatureNames();
}
