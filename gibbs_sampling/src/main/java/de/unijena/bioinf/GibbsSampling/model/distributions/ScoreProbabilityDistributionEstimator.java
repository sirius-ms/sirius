package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.MFCandidate;
import de.unijena.bioinf.GibbsSampling.model.distributions.ScoreProbabilityDistribution;

public class ScoreProbabilityDistributionEstimator implements EdgeScorer {
    private final EdgeScorer edgeScorer;
    private ScoreProbabilityDistribution scoreProbabilityDistribution;

    public ScoreProbabilityDistributionEstimator(EdgeScorer edgeScorer, ScoreProbabilityDistribution distribution) {
        this.edgeScorer = edgeScorer;
        this.scoreProbabilityDistribution = distribution;
    }

    public void prepare(MFCandidate[][] candidates) {
        this.edgeScorer.prepare(candidates);
        char numberOfSamples = '썐';
        HighQualityRandom random = new HighQualityRandom();
        double[] sampledScores = new double['썐'];

        for(int i = 0; i < '썐'; ++i) {
            int color1 = random.nextInt(candidates.length);
            int color2 = random.nextInt(candidates.length - 1);
            if(color2 >= color1) {
                ++color2;
            }

            int mf1 = random.nextInt(candidates[color1].length);
            int mf2 = random.nextInt(candidates[color2].length);
            sampledScores[i] = this.edgeScorer.score(candidates[color1][mf1], candidates[color2][mf2]);
        }

        this.scoreProbabilityDistribution.estimateDistribution(sampledScores);
    }

    public double score(MFCandidate candidate1, MFCandidate candidate2) {
        double score = this.edgeScorer.score(candidate1, candidate2);
        double prob = this.scoreProbabilityDistribution.toPvalue(score);
        return prob;
    }

    public ScoreProbabilityDistribution getProbabilityDistribution() {
        return this.scoreProbabilityDistribution;
    }

    public void clean() {
        this.edgeScorer.clean();
    }

    public double[] normalization(MFCandidate[][] candidates) {
        return new double[0];
    }
}
