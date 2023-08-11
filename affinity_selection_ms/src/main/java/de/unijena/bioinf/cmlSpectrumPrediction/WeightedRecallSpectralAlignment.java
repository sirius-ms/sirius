package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;

import java.util.HashSet;

public class WeightedRecallSpectralAlignment extends AbstractSpectralAlignment {

    private final HashSet<Peak> matchedMeasuredPeaks;

    public WeightedRecallSpectralAlignment(Deviation deviation){
        super(deviation);
        this.matchedMeasuredPeaks = new HashSet<>();
    }
    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> predictedSpectrum, OrderedSpectrum<Peak> measuredSpectrum) {
        this.matchedMeasuredPeaks.clear();
        if(predictedSpectrum.isEmpty() || measuredSpectrum.isEmpty()) return new SpectralSimilarity(0d, 0);

        double sum = 0d;
        for(Peak measuredPeak : measuredSpectrum) sum += measuredPeak.getIntensity();
        SpectralSimilarity spectralSimilarity = this.scoreAllAgainstAll(predictedSpectrum, measuredSpectrum);
        return new SpectralSimilarity(spectralSimilarity.similarity / sum, spectralSimilarity.shardPeaks);
    }

    @Override
    protected double scorePeaks(Peak predictedPeak, Peak measuredPeak) {
        if(!this.matchedMeasuredPeaks.contains(measuredPeak)){
            this.matchedMeasuredPeaks.add(measuredPeak);
            return measuredPeak.getIntensity();
        }
        return 0d;
    }

    @Override
    protected double maxAllowedDifference(double mz) {
        return this.deviation.absoluteFor(mz);
    }
}
