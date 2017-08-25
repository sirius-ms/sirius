package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;

public class ScoreProbabilityDistributionFix<C extends Candidate<?>> extends ScoreProbabilityDistributionEstimator<C> {

    /**
     *
     * @param edgeScorer
     * @param distribution has to be estimated beforehand!!!!
     */
    public ScoreProbabilityDistributionFix(EdgeScorer<C> edgeScorer, ScoreProbabilityDistribution distribution) {
        super(edgeScorer, distribution);
    }


    @Override
    public void setThreshold(double threshold) {
        throw new NoSuchMethodError();
    }

    @Override
    public void prepare(C[][] candidates) {
        edgeScorer.setThreshold(scoreProbabilityDistribution.getThreshold());
        edgeScorer.prepare(candidates);
    }

    @Override
    public double score(C candidate1, C candidate2) {
        double score = this.edgeScorer.score(candidate1, candidate2);
        double prob = this.scoreProbabilityDistribution.toPvalue(score);
        return prob;
    }

    @Override
    public double scoreWithoutThreshold(C candidate1, C candidate2) {
        double score = this.edgeScorer.scoreWithoutThreshold(candidate1, candidate2);
        double prob = this.scoreProbabilityDistribution.toPvalue(score);
        return prob;
    }

    @Override
    public void clean() {
        this.edgeScorer.clean();
    }

    @Override
    public double[] normalization(C[][] var1) {
        return new double[0];
    }
}
