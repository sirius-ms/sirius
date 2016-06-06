package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;

/**
 * Created by Marcus Ludwig on 22.04.16.
 */
public class MassFeature implements FeatureCreator {
    public MassFeature(){

    }

    @Override
    public void prepare(FingerprintStatistics statistics) {

    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        return new double[]{MolecularFormula.parse(query.getFormula()).getMass()};
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public boolean isCompatible(Query query, Candidate[] rankedCandidates) {
        return true;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[]{"mass"};
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
