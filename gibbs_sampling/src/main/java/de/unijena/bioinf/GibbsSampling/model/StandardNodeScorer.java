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

            Candidate candidate;
            double score;
            for(int j = 0; j < length; ++j) {
                candidate = currentCandidates[j];
                score = candidate.getScore();
                if(score > max) {
                    max = score;
                }
            }

            if(!this.normalize) {
                for(int j = 0; j < length; ++j) {
                    candidate = currentCandidates[j];
                    score = candidate.getScore();
                    candidate.addNodeProbabilityScore(Math.exp(this.gamma * (score - max)));
                }
            } else {
                double sum = 0.0D;
                double[] scores = new double[currentCandidates.length];

                Candidate candidate1;
                for(int j = 0; j < currentCandidates.length; ++j) {
                    candidate1 = currentCandidates[j];
                    double expS = Math.exp(this.gamma * (candidate1.getScore() - max));
                    sum += expS;
                    scores[j] = expS;
                }

                for(int j = 0; j < currentCandidates.length; ++j) {
                    candidate1 = currentCandidates[j];
                    candidate1.addNodeProbabilityScore(scores[j] / sum);
                }
            }
        }

    }
}
