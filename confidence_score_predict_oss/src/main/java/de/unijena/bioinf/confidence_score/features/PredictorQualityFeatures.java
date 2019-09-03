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
public class PredictorQualityFeatures implements FeatureCreator{


    PredictionPerformance[] statistics;

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics=statistics;
    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {

    PredictionPerformance.averageF1(statistics);
    int f1Below33=0;
    int f1Below66=0;
    int f1Below80=0;

    for (int i=0;i<statistics.length;i++){
        if(statistics[i].getF()>0 && statistics[i].getF()<=0.33){
            f1Below33+=1;

        }
        if(statistics[i].getF()>0.33 && statistics[i].getF()<=0.66){

            f1Below66+=1;

        }
        if(statistics[i].getF()>0.66 && statistics[i].getF()<=0.8){

            f1Below80+=1;
        }

    }
    return null;

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


        String[] name = new String[getFeatureSize()];
        name[0] = "AverageF1";
        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
