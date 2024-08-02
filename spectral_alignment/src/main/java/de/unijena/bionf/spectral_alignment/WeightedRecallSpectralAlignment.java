package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

import java.util.List;

public class WeightedRecallSpectralAlignment extends RecallSpectralAlignment{

    public WeightedRecallSpectralAlignment(Deviation deviation) {
        super(deviation);
    }

    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        List<Peak> matchedMsrdPeaks = this.getMatchedMsrdPeaks(msrdSpectrum, predSpectrum);

        double matchedIntensitySum = 0d, intensitySum = 0d;
        for(Peak p : matchedMsrdPeaks) matchedIntensitySum += p.getIntensity();
        for(Peak p : msrdSpectrum) intensitySum += p.getIntensity();

        double weightedRecall = (msrdSpectrum.isEmpty() || predSpectrum.isEmpty()) ? 0d : matchedIntensitySum / intensitySum;
        return new SpectralSimilarity(weightedRecall, matchedMsrdPeaks.size());
    }
}
