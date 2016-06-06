package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;

/**
 * Created by Marcus Ludwig on 30.04.16.
 */
public class NumOfCandidatesCounter implements FeatureCreator {

    @Override
    public void prepare(FingerprintStatistics statistics) {

    }

    @Override
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates) {
        return new double[]{rankedCandidates.length};
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
        return new String[]{"numOfCandidates"};
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper parameterHelper, DataDocument<G, D, L> dataDocument, D d) {

    }
}
