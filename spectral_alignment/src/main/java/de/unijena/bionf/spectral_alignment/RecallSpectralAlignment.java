package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

import java.util.ArrayList;
import java.util.List;

public class RecallSpectralAlignment extends AbstractSpectralMatching {

    public RecallSpectralAlignment(Deviation deviation){
        super(deviation);
    }
    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum, double msrdPrecursor, double predPrecursor) {
        return this.score(msrdSpectrum, predSpectrum);
    }

    public SpectralSimilarity score(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        List<Peak> matchedMsrdPeaks = this.getMatchedMsrdPeaks(msrdSpectrum, predSpectrum);
        double recall = (msrdSpectrum.isEmpty() || predSpectrum.isEmpty()) ? 0d : (double) matchedMsrdPeaks.size() / msrdSpectrum.size();
        return new SpectralSimilarity(recall, matchedMsrdPeaks.size());
    }

    public List<Peak> getMatchedMsrdPeaks(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        ArrayList<Peak> matchedMsrdPeaks = new ArrayList<>();
        int i = 0, j = 0;
        while(i < msrdSpectrum.size() && j < predSpectrum.size()){
            Peak msrdPeak = msrdSpectrum.getPeakAt(i);
            Peak predPeak = predSpectrum.getPeakAt(j);

            double absDiff = Math.abs(msrdPeak.getMass() - predPeak.getMass());
            double maxAllowedDiff = this.deviation.absoluteFor(Math.min(msrdPeak.getMass(), predPeak.getMass()));

            // Check if msrdPeak and predPeak are matching
            if(absDiff <= maxAllowedDiff){
                matchedMsrdPeaks.add(msrdPeak);
                i++;
            }else{
                if(msrdPeak.getMass() < predPeak.getMass()){
                    i++;
                }else{
                    j++;
                }
            }
        }
        return matchedMsrdPeaks;
    }

}
