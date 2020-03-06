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
 * Created by Marcus Ludwig on 07.03.16.
 */
public class AllConfidenceScoreDiffHitFeatures implements FeatureCreator {
    private final String[] names;
    private FingerblastScoring scoring;
    private PredictionPerformance[] statistics;
    Scored<FingerprintCandidate>[] rankedCandidates;
    double conf;
    boolean same;
    public AllConfidenceScoreDiffHitFeatures(double conf, boolean same){
        this.rankedCandidates=rankedCandidates;
        names = new String[]{"AllConfScoreDiffHit"};
        this.scoring=scoring;
        this.conf=conf;
        this.same=same;
    }


    //TODO: Also code in that this is different for same pubchem hit - different pubchem hit

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public int weight_direction() {
        return -1;
    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {


        final double[] scores = new double[1];
        if(!same)
            scores[0] = conf;

        else scores[0]=0.5;

        return scores;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(ProbabilityFingerprint query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
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
