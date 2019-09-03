package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by martin on 16.07.18.
 */
public class ScoreDiffScorerFeatures implements FeatureCreator {

    Scored<FingerprintCandidate> best_hit_scorer1;

    Scored<FingerprintCandidate> best_hit_scorer2;

    FingerblastScoring scoring;


    /**
     *
     */
    public ScoreDiffScorerFeatures(Scored<FingerprintCandidate> hit1, Scored<FingerprintCandidate> hit2, FingerblastScoring scoring){
        this.best_hit_scorer1=hit1;
        this.best_hit_scorer2=hit2;

        this.scoring=scoring;


    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {
        double[] distance = new double[1];

        scoring.prepare(query);

        distance[0]=Math.abs(scoring.score(query,best_hit_scorer1.getCandidate().getFingerprint())-scoring.score(query,best_hit_scorer2.getCandidate().getFingerprint()));




        return distance;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(ProbabilityFingerprint query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return false;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 0;
    }

    @Override
    public String[] getFeatureNames() {

        String[] name = new String[1];
        name[0] = "scorediffscorer";

        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
