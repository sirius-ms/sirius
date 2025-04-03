package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import it.unimi.dsi.fastutil.ints.IntList;

public class WeightedRecallSpectralAlignment extends RecallSpectralAlignment{

    public WeightedRecallSpectralAlignment(Deviation deviation) {
        super(deviation);
    }

    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        IntList matchedPeaks = this.getMatchedPeaks(msrdSpectrum, predSpectrum);

        double matchedIntensitySum = 0d, intensitySum = 0d;

        for (int i = 0; i < matchedPeaks.size(); i += 2)
            matchedIntensitySum += msrdSpectrum.getPeakAt(matchedPeaks.getInt(i)).getIntensity();
        for(Peak p : msrdSpectrum)
            intensitySum += p.getIntensity();

        double weightedRecall = (msrdSpectrum.isEmpty() || predSpectrum.isEmpty()) ? 0d : matchedIntensitySum / intensitySum;
        return new SpectralSimilarity(weightedRecall, matchedPeaks);
    }
}
