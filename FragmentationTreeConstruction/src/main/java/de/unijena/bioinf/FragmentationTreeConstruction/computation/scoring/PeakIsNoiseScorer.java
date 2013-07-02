package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.math.*;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

@Called("Intensity")
public class PeakIsNoiseScorer implements PeakScorer {

    private ByMedianEstimatable<? extends RealDistribution> distribution;

    public ByMedianEstimatable<? extends RealDistribution> getDistribution() {
        return distribution;
    }

    public void setDistribution(ByMedianEstimatable<? extends RealDistribution> distribution) {
        this.distribution = distribution;
    }

    public PeakIsNoiseScorer() {
        this(ParetoDistribution.getMedianEstimator(0.005));
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

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.distribution = (ByMedianEstimatable<RealDistribution>)helper.unwrap(document, document.getFromDictionary(dictionary, "distribution"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "distribution", helper.wrap(document,distribution));
    }
}
