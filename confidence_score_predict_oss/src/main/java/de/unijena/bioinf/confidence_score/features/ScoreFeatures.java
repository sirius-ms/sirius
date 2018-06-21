package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.fingerid.blast.*;
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class ScoreFeatures implements FeatureCreator {
    private final String[] names;
    private final FingerblastScoring[] scorers;
    private PredictionPerformance[] statistics;

    public ScoreFeatures(){
        names = new String[]{"CSIFingerIdScoring", "CovarianceScoring"};
        scorers = new FingerblastScoring[2];
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates, IdentificationResult idresult) {
        scorers[0] = new CSIFingerIdScoring(statistics);
     //   scorers[1] = CovarianceScoring.readScoringFromFile();//TODO: How to get this?

        final CompoundWithAbstractFP<Fingerprint> topHit = rankedCandidates[0];
        final double[] scores = new double[scorers.length];
        for (int i = 0; i < scorers.length; i++) {
            scorers[i].prepare(query.getFingerprint());
            scores[i] = scorers[i].score(query.getFingerprint(), topHit.getFingerprint());
        }
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return scorers.length;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return rankedCandidates.length>0;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 1;
    }

    @Override
    public String[] getFeatureNames() {
        return names;
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
