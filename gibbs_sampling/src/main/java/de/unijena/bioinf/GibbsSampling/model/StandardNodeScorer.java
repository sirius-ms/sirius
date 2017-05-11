package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.MFCandidate;
import de.unijena.bioinf.GibbsSampling.model.NodeScorer;

public class StandardNodeScorer implements NodeScorer {
    private final boolean normalize;
    private final double gamma;

    public StandardNodeScorer() {
        this(false, 0.1D);
    }

    public StandardNodeScorer(boolean normalizeToOne, double gamma) {
        this.normalize = normalizeToOne;
        this.gamma = gamma;
    }

    public void score(MFCandidate[][] candidates) {
        for(int i = 0; i < candidates.length; ++i) {
            MFCandidate[] mfCandidates = candidates[i];
            double max = -1.0D / 0.0;
            MFCandidate[] sum = mfCandidates;
            int var7 = mfCandidates.length;

            int expScore;
            MFCandidate candidate;
            double score;
            for(expScore = 0; expScore < var7; ++expScore) {
                candidate = sum[expScore];
                score = candidate.getScore();
                if(score > max) {
                    max = score;
                }
            }

            if(!this.normalize) {
                sum = mfCandidates;
                var7 = mfCandidates.length;

                for(expScore = 0; expScore < var7; ++expScore) {
                    candidate = sum[expScore];
                    score = candidate.getScore();
                    candidate.addNodeProbabilityScore(Math.exp(this.gamma * (score - max)));
                }
            } else {
                double var16 = 0.0D;
                double[] var17 = new double[mfCandidates.length];
                int var18 = 0;
                MFCandidate[] var19 = mfCandidates;
                int var11 = mfCandidates.length;

                int var12;
                MFCandidate candidate1;
                for(var12 = 0; var12 < var11; ++var12) {
                    candidate1 = var19[var12];
                    double expS = Math.exp(this.gamma * (candidate1.getScore() - max));
                    var16 += expS;
                    var17[var18++] = expS;
                }

                var18 = 0;
                var19 = mfCandidates;
                var11 = mfCandidates.length;

                for(var12 = 0; var12 < var11; ++var12) {
                    candidate1 = var19[var12];
                    candidate1.addNodeProbabilityScore(var17[var18++] / var16);
                }
            }
        }

    }
}
