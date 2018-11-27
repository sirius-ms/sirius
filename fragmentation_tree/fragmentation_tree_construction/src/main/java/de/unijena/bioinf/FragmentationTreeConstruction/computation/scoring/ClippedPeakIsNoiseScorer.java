package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.ByMedianEstimatable;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;
import de.unijena.bioinf.ChemistryBase.ms.MedianNoiseIntensity;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public class ClippedPeakIsNoiseScorer implements PeakScorer {

    private ByMedianEstimatable<? extends RealDistribution> distribution;
    private double beta;

    public ByMedianEstimatable<? extends RealDistribution> getDistribution() {
        return distribution;
    }

    public void setDistribution(ByMedianEstimatable<? extends RealDistribution> distribution) {
        this.distribution = distribution;
    }

    public ClippedPeakIsNoiseScorer() {
        this(ParetoDistribution.getMedianEstimator(0.005));
    }

    public ClippedPeakIsNoiseScorer(ByMedianEstimatable<? extends RealDistribution> distribution) {
        this.distribution = distribution;
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        final RealDistribution estimatedDistribution = distribution.extimateByMedian(
                        input.getExperimentInformation().getAnnotationOrDefault(MedianNoiseIntensity.class).value);

        double maxIntensity = 0d;
        for (ProcessedPeak p : input.getMergedPeaks()) maxIntensity = Math.max(p.getRelativeIntensity(), maxIntensity);
        if (maxIntensity<=0) return;
        final double clipping = 1d - estimatedDistribution.getCumulativeProbability(1d);

        for (int i=0; i < peaks.size(); ++i) {
            if (peaks.get(i).getRelativeIntensity()<=0) continue;
            final double peakIntensity = peaks.get(i).getRelativeIntensity()/maxIntensity;
            final double noiseProbability = 1d-estimatedDistribution.getCumulativeProbability(peakIntensity);
            final double clippingCorrection = (noiseProbability-clipping+beta)/(1-clipping+beta);
            scores[i] -= Math.log(clippingCorrection);
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.distribution = (ByMedianEstimatable<RealDistribution>)helper.unwrap(document, document.getFromDictionary(dictionary, "distribution"));
        this.beta = document.getDoubleFromDictionary(dictionary, "beta");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "distribution", helper.wrap(document,distribution));
        document.addToDictionary(dictionary, "beta", beta);
    }
}
