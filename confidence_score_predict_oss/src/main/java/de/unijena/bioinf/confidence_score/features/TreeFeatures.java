package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by martin on 20.06.18.
 */
public class TreeFeatures implements FeatureCreator {
    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query,  IdentificationResult idresult,long flags) {

       /* double[] scores= new double[4];
        TreeScoring current_tree_scores =  idresult.getRawTree().getAnnotationOrThrow(TreeScoring.class);

        scores[0]=current_tree_scores.getExplainedIntensityOfExplainablePeaks();
        scores[1]= current_tree_scores.getExplainedIntensity();
        scores[2]=current_tree_scores.getRatioOfExplainedPeaks();
        scores[3]= current_tree_scores.getOverallScore();

        return scores;*/
        throw new IllegalArgumentException("This method has to be reimplemented to work with new sirius api");

    }

    @Override
    public int getFeatureSize() {
        return 4;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return false;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 0;
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        names[0] = "explIntExplPeaks";
        names[1] = "explInt";
        names[2] = "ratioExplPeaks";
        names[3] = "score";

        return names;

    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
