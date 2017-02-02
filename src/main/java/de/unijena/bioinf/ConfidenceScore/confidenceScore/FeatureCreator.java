package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

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
    double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates);

    int getFeatureSize();

    boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates);

    int getRequiredCandidateSize();

    String[] getFeatureNames();
}
