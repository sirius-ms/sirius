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
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by martin on 27.06.18.
 */
public class PredictionQualityFeatures implements FeatureCreator{


    public int weight_direction=1;
    PredictionPerformance[] statistics;
    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics=statistics;
    }

    @Override
    public int weight_direction() {
        return weight_direction;
    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {

        double[] qualityReturn = new double[1];

        double quality=0;
        double[] prob_fpt= query.toProbabilityArray();

        for(int i=0;i<prob_fpt.length;i++){
            quality+=(Math.max(1-prob_fpt[i],prob_fpt[i]));



        }

        qualityReturn[0]=quality/query.cardinality();



        return qualityReturn;
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
        name[0] = "PredicitionQuality";
        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
