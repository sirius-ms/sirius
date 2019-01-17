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
    public ScoreProbabilityDistributionFix(EdgeScorer<C> edgeScorer, ScoreProbabilityDistribution distribution, double percentageOfEdgesBelowThreshold) {
        super(edgeScorer, distribution, percentageOfEdgesBelowThreshold);
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
    public double[] normalization(C[][] var1, double minimum_number_matched_peaks_losses) {
        return new double[0];
    }
}
