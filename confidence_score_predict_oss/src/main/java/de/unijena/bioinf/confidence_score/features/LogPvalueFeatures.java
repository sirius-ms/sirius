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
 * Created by martin on 20.06.18.
 */
public class LogPvalueFeatures implements FeatureCreator {
    Scored<FingerprintCandidate>[] rankedCandidates;
    Scored<FingerprintCandidate>[] rankedCandidates_filtered;

    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    public LogPvalueFeatures(Scored<FingerprintCandidate>[] rankedCandidates,Scored<FingerprintCandidate>[] rankedCandidates_filtered){
        this.rankedCandidates=rankedCandidates;
        this.rankedCandidates_filtered=rankedCandidates_filtered;
    }



    @Override
    public double[] computeFeatures(ProbabilityFingerprint query,  IdentificationResult idresult) {
        double[] return_value =  new double[2];


        PvalueScoreUtils utils= new PvalueScoreUtils();





        double pvalue= utils.computePvalueScore(rankedCandidates,rankedCandidates_filtered,rankedCandidates_filtered[0]);


        double pvalue_kde=100;
        if(rankedCandidates.length>5) pvalue_kde = utils.compute_pvalue_with_KDE(rankedCandidates,rankedCandidates_filtered,rankedCandidates_filtered[0]);



        if(pvalue==0){
            return_value[0]=-20;

        }else {
            return_value[0]  = Math.log(pvalue);

        }

        if(pvalue_kde==0){
            return_value[1]=-20;

        }else {
            return_value[1]= Math.log(pvalue_kde);
        }



        return return_value;
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
        name[0]="LogpvalueScore";
        name[1]="LogPvalueScoreKDE";
        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
