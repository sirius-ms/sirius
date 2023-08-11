package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;

import java.util.HashSet;
import java.util.List;

public class RecallSpectralAlignment extends AbstractSpectralAlignment {

    private final HashSet<Peak> matchedMeasuredPeaks;

    public RecallSpectralAlignment(Deviation deviation){
        super(deviation);
        this.matchedMeasuredPeaks = new HashSet<>();
    }
    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> predictedSpectrum, OrderedSpectrum<Peak> measuredSpectrum){
        if(predictedSpectrum.isEmpty() || measuredSpectrum.isEmpty()) return new SpectralSimilarity(0d, 0);
        this.matchedMeasuredPeaks.clear(); // necessary to maintain the functionality
        SpectralSimilarity specSimilarity = this.scoreAllAgainstAll(predictedSpectrum, measuredSpectrum);// similarity := #MatchedPeaksInMeasuredSpectrum; shardPeaks := #SharedPeaksInBoth
        return new SpectralSimilarity(specSimilarity.similarity / measuredSpectrum.size(), specSimilarity.shardPeaks);
    }

    @Override
    protected double scorePeaks(Peak predictedPeak, Peak measuredPeak) {
        if(!this.matchedMeasuredPeaks.contains(measuredPeak)){
            this.matchedMeasuredPeaks.add(measuredPeak);
            return 1d;
        }
        return 0d;
    }

    @Override
    protected double maxAllowedDifference(double mz) {
        return this.deviation.absoluteFor(mz);
    }

    public List<Peak> getPreviousMatchedMeasuredPeaks(){
        return this.matchedMeasuredPeaks.stream().toList();
    }
}
