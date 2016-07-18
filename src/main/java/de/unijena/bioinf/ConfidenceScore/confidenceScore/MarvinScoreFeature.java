package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;

/**
 * Created by Marcus Ludwig on 21.04.16.
 */
public class MarvinScoreFeature implements FeatureCreator {
    private final String name;
    private FingerblastScoring scorer;

    public MarvinScoreFeature(){
        name = "CSIFingerIdScoring";
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        scorer = new CSIFingerIdScoring(statistics);
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        final CompoundWithAbstractFP<Fingerprint> topHit = rankedCandidates[0];
        final double[] scores = new double[1];
        scorer.prepare(query.getFingerprint());
        scores[0] = scorer.score(query.getFingerprint(), topHit.getFingerprint());
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return rankedCandidates.length>0;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[]{name};
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //Nothing to do as long as ScoringMethods stay the same
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //Nothing to do as long as ScoringMethods stay the same
    }
}
