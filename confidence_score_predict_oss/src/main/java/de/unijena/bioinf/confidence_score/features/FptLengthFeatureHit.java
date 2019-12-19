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
 * Created by martin on 16.07.18.
 */
public class FptLengthFeatureHit implements FeatureCreator {



    Scored<FingerprintCandidate>[] rankedCandidates_filtered;

    public FptLengthFeatureHit(Scored<FingerprintCandidate>[] rankedCandidates_filtered){
        this.rankedCandidates_filtered=rankedCandidates_filtered;


    }
    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public int weight_direction() {
        return 1;
    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {

        double[] length =  new double[1];

        length[0]= rankedCandidates_filtered[0].getCandidate().getFingerprint().cardinality();

        return length;
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
        name[0] = "fptLengthHit";
        return name;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
