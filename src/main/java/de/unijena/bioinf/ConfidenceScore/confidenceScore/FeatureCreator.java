package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.fingerid.Candidate;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import de.unijena.bioinf.fingerid.Query;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public interface FeatureCreator extends Parameterized {

    public void prepare(FingerprintStatistics statistics);

    /**
     *
     * @param query
     * @param rankedCandidates sorted best to worst hit!
     * @return
     */
    public double[] computeFeatures(Query query, Candidate[] rankedCandidates);

    public int getFeatureSize();

    public boolean isCompatible(Query query, Candidate[] rankedCandidates);

    public String[] getFeatureNames();
}
