package de.unijena.bioinf.ConfidenceScore.confidenceScore;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.FingerblastScoring;
import de.unijena.bioinf.fingerid.blast.ProbabilityEstimateScoring;
import de.unijena.bioinf.fingerid.blast.SimpleMaximumLikelihoodScoring;

/**
 * Created by Marcus Ludwig on 07.03.16.
 */
public class ScoreDifferenceFeatures implements FeatureCreator {
    private final String[] names;
    private final FingerblastScoring[] scorers;
    private int[] positions;
    private int max;
    private PredictionPerformance[] statistics;

    public ScoreDifferenceFeatures(){
        this(new int[]{});
    }

    /**
     *
     * @param positions the positions
     */
    public ScoreDifferenceFeatures(int... positions){
        names = new String[]{"CSIFingerIdScoring", "SimpleMaximumLikelihoodScoring", "ProbabilityEstimateScoring"};
        scorers = new FingerblastScoring[3];

        this.positions = positions;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < positions.length; i++) max = Math.max(max, positions[i]);
        this.max = max;
    }

    @Override
    public void prepare(PredictionPerformance[] statistics) {
        this.statistics = statistics;
    }

    @Override
    public double[] computeFeatures(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        scorers[0] = new CSIFingerIdScoring(statistics);
        scorers[1] = new SimpleMaximumLikelihoodScoring(statistics);
        scorers[2] = new ProbabilityEstimateScoring(statistics);

        double[] scores = new double[scorers.length * positions.length];
        final CompoundWithAbstractFP<Fingerprint> topHit = rankedCandidates[0];
        int pos = 0;
        for (int i = 0; i < scorers.length; i++) {
            scorers[i].prepare(query.getFingerprint());
            final double topScore = scorers[i].score(query.getFingerprint(), topHit.getFingerprint());
            for (int j = 0; j < positions.length; j++) {
                scores[pos++] = topScore - scorers[i].score(query.getFingerprint(), rankedCandidates[positions[j]].getFingerprint());
            }
        }
        assert pos == scores.length;
        return scores;
    }

    @Override
    public int getFeatureSize() {
        return scorers.length*positions.length;
    }

    @Override
    public boolean isCompatible(CompoundWithAbstractFP<ProbabilityFingerprint> query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return (rankedCandidates.length>max);
    }

    @Override
    public int getRequiredCandidateSize() {
        return max+1;
    }

    @Override
    public String[] getFeatureNames() {
        String[] names = new String[getFeatureSize()];
        int pos = 0;
        for (int i = 0; i < scorers.length; i++) {
            String scoringMethod = this.names[i];
            for (int j = 0; j < positions.length; j++) {
                int position = positions[j];
                names[pos++] = scoringMethod+"DiffTo"+position;
            }
        }
        return names;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L positionsList = document.getListFromDictionary(dictionary, "positions");
        int size = document.sizeOfList(positionsList);
        int[] positions = new int[size];
        for (int i = 0; i < size; i++) positions[i] = (int)document.getIntFromList(positionsList, i);

        this.positions = positions;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < positions.length; i++) max = Math.max(max, positions[i]);
        this.max = max;
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        L list = document.newList();
        for (int position : positions) document.addToList(list, position);
        document.addListToDictionary(dictionary, "positions", list);
    }
}
