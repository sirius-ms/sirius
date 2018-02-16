package de.unijena.bioinf.GibbsSampling.model;

import java.util.Arrays;

public class RankNodeScorer implements NodeScorer {
    private final double topScore;
    private final double lambda;
    private final boolean normalize;

    public RankNodeScorer(double topScore, double lambda, boolean normalizeToOne) {
        this.topScore = topScore;
        this.lambda = lambda;
        this.normalize = normalizeToOne;
    }

    public RankNodeScorer() {
        this(1.0D, 0.1D, false);
    }

    public RankNodeScorer(double topScore) {
        this(topScore, 0.1D, false);
    }

    public void score(Candidate[] candidates) {
        Candidate[] currentCandidates = candidates.clone();
        Arrays.sort(currentCandidates);
        if(!this.normalize) {
            for(int j = 0; j < currentCandidates.length; ++j) {
                Candidate candidate = currentCandidates[j];
                candidate.addNodeProbabilityScore(this.score(j));
            }
        } else {
            double sum = 0.0D;
            double[] scores = new double[currentCandidates.length];

            int j;
            for(j = 0; j < currentCandidates.length; ++j) {
                double s = this.score(j);
                scores[j] = s;
                sum += s;
            }

            for(j = 0; j < currentCandidates.length; ++j) {
                currentCandidates[j].addNodeProbabilityScore(scores[j] / sum);
            }
        }

    }

    private double score(int rank) {
        return this.topScore * Math.exp(-this.lambda * (double)rank);
    }
}
