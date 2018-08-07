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
import de.unijena.bioinf.sirius.IdentificationResult;

/**
 * Created by martin on 27.06.18.
 */
public class PredictionQualityFeatures implements FeatureCreator{



    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, IdentificationResult idresult, long flags) {

        double[] qualityReturn = new double[1];

        double quality=0;
        double[] prob_fpt= query.getFingerprint().toProbabilityArray();

        for(int i=0;i<prob_fpt.length;i++){
            quality+=Math.max(1-prob_fpt[i],prob_fpt[i]);

            //this penalizes predictions close to 0.5 stronger because they

            if(prob_fpt[i]>0.4 && prob_fpt[i]<0.6){
                quality-=1;
            }
        }

        qualityReturn[0]=quality;



        return qualityReturn;
    }

    @Override
    public int getFeatureSize() {
        return 1;
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
