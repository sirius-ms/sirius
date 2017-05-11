package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.MFCandidate;
import de.unijena.bioinf.GibbsSampling.model.NodeScorer;
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

    public void score(MFCandidate[][] candidates) {
        for(int i = 0; i < candidates.length; ++i) {
            MFCandidate[] mfCandidates = (MFCandidate[])candidates[i].clone();
            Arrays.sort(mfCandidates);
            if(!this.normalize) {
                for(int var11 = 0; var11 < mfCandidates.length; ++var11) {
                    MFCandidate candidate = mfCandidates[var11];
                    candidate.addNodeProbabilityScore(this.score(var11));
                }
            } else {
                double j = 0.0D;
                double[] scores = new double[mfCandidates.length];

                int j1;
                for(j1 = 0; j1 < mfCandidates.length; ++j1) {
                    MFCandidate var10000 = mfCandidates[j1];
                    double s = this.score(j1);
                    scores[j1] = s;
                    j += s;
                }

                for(j1 = 0; j1 < mfCandidates.length; ++j1) {
                    mfCandidates[j1].addNodeProbabilityScore(scores[j1] / j);
                }
            }
        }

    }

    private double score(int rank) {
        return this.topScore * Math.exp(-this.lambda * (double)rank);
    }
}
