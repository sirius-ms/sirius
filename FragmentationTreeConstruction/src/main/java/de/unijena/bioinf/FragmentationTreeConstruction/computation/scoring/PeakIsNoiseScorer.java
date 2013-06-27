package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.math.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

@Called("Intensity")
public class PeakIsNoiseScorer implements PeakScorer {

    public static PeakIsNoiseScorer fromPareto(double k) {
        return new PeakIsNoiseScorer(new ParetoDistribution(k, 0.005));
    }

    private ByMedianEstimatable<? extends RealDistribution> distribution;

    public ByMedianEstimatable<? extends RealDistribution> getDistribution() {
        return distribution;
    }

    public void setDistribution(ByMedianEstimatable<? extends RealDistribution> distribution) {
        this.distribution = distribution;
    }

    public PeakIsNoiseScorer(double lambda) {
        this(ExponentialDistribution.fromLambda(lambda));
    }

    public PeakIsNoiseScorer(ByMedianEstimatable<? extends RealDistribution> distribution) {
        this.distribution = distribution;
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        final RealDistribution estimatedDistribution = distribution.extimateByMedian(input.getExperimentInformation().getMeasurementProfile().getMedianNoiseIntensity());
        for (int i=0; i < peaks.size(); ++i) {
            scores[i] -= estimatedDistribution.getInverseLogCumulativeProbability(peaks.get(i).getRelativeIntensity());
        }
    }
}
