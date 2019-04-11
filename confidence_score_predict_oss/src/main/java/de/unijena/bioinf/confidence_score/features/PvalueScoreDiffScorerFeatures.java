package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
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
public class PvalueScoreDiffScorerFeatures implements FeatureCreator {

    Scored<FingerprintCandidate>[] rankedCands;

    Scored<FingerprintCandidate> best_hit_scorer;

    FingerblastScoring scoring;
    long flags=-1;


    /**
     * rescores the best hit of a scorer with a different scorer, than calculates pvalue and compares it to the best hit of the 2nd scorer
     */
    public PvalueScoreDiffScorerFeatures(Scored<FingerprintCandidate>[] rankedCands, Scored<FingerprintCandidate> hit, FingerblastScoring scoring){
        this.rankedCands=rankedCands;
        this.best_hit_scorer=hit;

        this.scoring=scoring;


    }

    public PvalueScoreDiffScorerFeatures(Scored<FingerprintCandidate>[] rankedCands, Scored<FingerprintCandidate> hit, FingerblastScoring scoring, long flags){
        this.rankedCands=rankedCands;
        this.best_hit_scorer=hit;
        this.flags=flags;
        this.scoring=scoring;


    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult, long flags) {
        double[] pvalueScore = new double[2];


        if(this.flags==-1)this.flags=flags;
        scoring.prepare(query);

        double score = scoring.score(query,best_hit_scorer.getCandidate().getFingerprint());

        Scored<FingerprintCandidate> current = new Scored<FingerprintCandidate>(best_hit_scorer.getCandidate(),score);

        PvalueScoreUtils utils = new PvalueScoreUtils();

        pvalueScore[0] = utils.computePvalueScore(rankedCands,current);

        pvalueScore[1] = Math.log(pvalueScore[0]);






        return pvalueScore;
    }

    @Override
    public int getFeatureSize() {
        return 2;
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

        String[] name = new String[2];
        name[0] = "pvaluescorediffscorer";
        name[1] = "pvaluescorediffscorer";
        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
