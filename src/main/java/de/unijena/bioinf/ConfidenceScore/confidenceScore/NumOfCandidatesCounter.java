package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;

/**
 * Created by Marcus Ludwig on 30.04.16.
 */
public class NumOfCandidatesCounter implements FeatureCreator {

    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return new double[]{rankedCandidates.length};
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return true;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 1;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[]{"numOfCandidates"};
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }
}
