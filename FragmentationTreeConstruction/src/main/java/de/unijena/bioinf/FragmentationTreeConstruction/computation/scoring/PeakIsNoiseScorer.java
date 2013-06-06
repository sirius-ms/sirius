package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
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
@Called("Intensity")
public class PeakIsNoiseScorer implements PeakScorer {

    public static PeakIsNoiseScorer fromPareto(double k) {
        return new PeakIsNoiseScorer(new ParetoDistribution(k, 0.005));
    }

    private RealDistribution distribution;

    public RealDistribution getDistribution() {
        return distribution;
    }

    public void setDistribution(RealDistribution distribution) {
        this.distribution = distribution;
    }

    public PeakIsNoiseScorer(double lambda) {
        this(ExponentialDistribution.fromLambda(lambda));
    }

    public PeakIsNoiseScorer(RealDistribution distribution) {
        this.distribution = distribution;
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        for (int i=0; i < peaks.size(); ++i) {
            scores[i] -= distribution.getInverseLogCumulativeProbability(peaks.get(i).getRelativeIntensity());
        }
    }
}
