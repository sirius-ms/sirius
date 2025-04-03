package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

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
        IntList matchedPeaks = this.getMatchedPeaks(msrdSpectrum, predSpectrum);
        double recall = (msrdSpectrum.isEmpty() || predSpectrum.isEmpty()) ? 0d : (double) matchedPeaks.size() / msrdSpectrum.size();
        return new SpectralSimilarity(recall, matchedPeaks);
    }

    public List<Peak> getMatchedMsrdPeaks(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        IntList matched = getMatchedPeaks(msrdSpectrum, predSpectrum);
        List<Peak> peaks = new ArrayList<>(matched.size() >> 1);
        for (int i = 0; i < matched.size(); i += 2)
            peaks.add(msrdSpectrum.getPeakAt(matched.getInt(i)));
        return peaks;
    }
    
    public IntList getMatchedPeaks(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        IntList matchedPeaks = new IntArrayList(Math.min(msrdSpectrum.size(), predSpectrum.size()));

        int i = 0, j = 0;
        while(i < msrdSpectrum.size() && j < predSpectrum.size()){
            Peak msrdPeak = msrdSpectrum.getPeakAt(i);
            Peak predPeak = predSpectrum.getPeakAt(j);

            double absDiff = Math.abs(msrdPeak.getMass() - predPeak.getMass());
            double maxAllowedDiff = this.deviation.absoluteFor(Math.min(msrdPeak.getMass(), predPeak.getMass()));

            // Check if msrdPeak and predPeak are matching
            if(absDiff <= maxAllowedDiff){
                matchedPeaks.add(i);
                matchedPeaks.add(j);
                i++;
            }else{
                if(msrdPeak.getMass() < predPeak.getMass()){
                    i++;
                }else{
                    j++;
                }
            }
        }
        return matchedPeaks;
    }
}
