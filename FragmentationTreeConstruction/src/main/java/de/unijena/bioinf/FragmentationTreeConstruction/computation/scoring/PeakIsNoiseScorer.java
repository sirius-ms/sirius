package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 4/18/13
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class PeakIsNoiseScorer implements PeakScorer {

    private final RealDistribution distribution;

    public PeakIsNoiseScorer(double lambda) {
        this(ExponentialDistribution.fromLambda(lambda));
    }

    public PeakIsNoiseScorer(RealDistribution distribution) {
        this.distribution = distribution;
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        for (int i=0; i < peaks.size(); ++i) {
            scores[i] += distribution.getLogCumulativeProbability(peaks.get(i).getIntensity());
        }
    }
}
