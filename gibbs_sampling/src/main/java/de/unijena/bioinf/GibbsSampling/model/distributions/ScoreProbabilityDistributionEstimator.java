package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.ChemistryBase.math.HighQualityRandom;
import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;

public class ScoreProbabilityDistributionEstimator<C extends Candidate<?>> implements EdgeScorer<C> {
    private final EdgeScorer<C> edgeScorer;
    private ScoreProbabilityDistribution scoreProbabilityDistribution;

    public ScoreProbabilityDistributionEstimator(EdgeScorer<C> edgeScorer, ScoreProbabilityDistribution distribution) {
        this.edgeScorer = edgeScorer;
        this.scoreProbabilityDistribution = distribution;
    }

    public void prepare(C[][] candidates) {
        edgeScorer.prepare(candidates);
        int numberOfSamples = 50000;
        HighQualityRandom random = new HighQualityRandom();
        double[] sampledScores = new double[numberOfSamples];

        for(int i = 0; i < numberOfSamples; ++i) {
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


        //todo just if EdgeThresholdMinConnectionsFilter not used!!!!!!!!!!!!!!!
        //set adjusted threshold for scorer
//        edgeScorer.setThreshold(scoreProbabilityDistribution.getThreshold());
//        edgeScorer.prepare(candidates);
    }


    public void setThresholdAndPrepare(C[][] candidates) {
        edgeScorer.prepare(candidates);
        int numberOfSamples = 50000;
        HighQualityRandom random = new HighQualityRandom();
        double[] sampledScores = new double[numberOfSamples];

        for(int i = 0; i < numberOfSamples; ++i) {
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


        //todo just if EdgeThresholdMinConnectionsFilter not used!!!!!!!!!!!!!!!
        //set adjusted threshold for scorer
        edgeScorer.setThreshold(scoreProbabilityDistribution.getThreshold());
        edgeScorer.prepare(candidates);
    }

    @Override
    public void setThreshold(double threshold) {
        throw new NoSuchMethodError();
    }


    public double score(C candidate1, C candidate2) {
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

    public double[] normalization(C[][] candidates) {
        return new double[0];
    }
}
