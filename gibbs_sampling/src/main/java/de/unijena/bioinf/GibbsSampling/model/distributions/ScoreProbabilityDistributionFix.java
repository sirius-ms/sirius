package de.unijena.bioinf.GibbsSampling.model.distributions;

import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;

import java.util.Arrays;

public class ScoreProbabilityDistributionFix<C extends Candidate<?>> extends ScoreProbabilityDistributionEstimator<C> {

    /**
     *
     * @param edgeScorer
     * @param distribution has to be estimated beforehand!!!!
     */
    public ScoreProbabilityDistributionFix(EdgeScorer<C> edgeScorer, ScoreProbabilityDistribution distribution, double percentageOfEdgesToUse) {
        super(edgeScorer, distribution, percentageOfEdgesToUse);
    }


    @Override
    protected void estimateDistribution(double[] sampledScores) {
        //do nothing
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
