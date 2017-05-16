package de.unijena.bioinf.GibbsSampling.model;

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

    public void score(Candidate[][] candidates) {
        for(int i = 0; i < candidates.length; ++i) {
            Candidate[] currentCandidates = candidates[i];
            double max = -1.0D / 0.0;
            int length = currentCandidates.length;

            int expScore;
            Candidate candidate;
            double score;
            for(expScore = 0; expScore < length; ++expScore) {
                candidate = currentCandidates[expScore];
                score = candidate.getScore();
                if(score > max) {
                    max = score;
                }
            }

            if(!this.normalize) {
                for(expScore = 0; expScore < length; ++expScore) {
                    candidate = currentCandidates[expScore];
                    score = candidate.getScore();
                    candidate.addNodeProbabilityScore(Math.exp(this.gamma * (score - max)));
                }
            } else {
                double var16 = 0.0D;
                double[] var17 = new double[currentCandidates.length];
                int var18 = 0;
                Candidate[] var19 = currentCandidates;
                int var11 = currentCandidates.length;

                int var12;
                Candidate candidate1;
                for(var12 = 0; var12 < var11; ++var12) {
                    candidate1 = var19[var12];
                    double expS = Math.exp(this.gamma * (candidate1.getScore() - max));
                    var16 += expS;
                    var17[var18++] = expS;
                }

                var18 = 0;
                var19 = currentCandidates;
                var11 = currentCandidates.length;

                for(var12 = 0; var12 < var11; ++var12) {
                    candidate1 = var19[var12];
                    candidate1.addNodeProbabilityScore(var17[var18++] / var16);
                }
            }
        }

    }
}
