package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

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
        Int2IntMap matchedPeaks = this.getMatchedPeaks(msrdSpectrum, predSpectrum);
        double recall = (msrdSpectrum.isEmpty() || predSpectrum.isEmpty()) ? 0d : (double) matchedPeaks.size() / msrdSpectrum.size();
        return new SpectralSimilarity(recall, matchedPeaks);
    }

    public List<Peak> getMatchedMsrdPeaks(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        return getMatchedPeaks(msrdSpectrum, predSpectrum).keySet().intStream().mapToObj(msrdSpectrum::getPeakAt).toList();
    }
    
    public Int2IntMap getMatchedPeaks(OrderedSpectrum<Peak> msrdSpectrum, OrderedSpectrum<Peak> predSpectrum){
        Int2IntMap matchedPeaks = new Int2IntOpenHashMap();

        int i = 0, j = 0;
        while(i < msrdSpectrum.size() && j < predSpectrum.size()){
            Peak msrdPeak = msrdSpectrum.getPeakAt(i);
            Peak predPeak = predSpectrum.getPeakAt(j);

            double absDiff = Math.abs(msrdPeak.getMass() - predPeak.getMass());
            double maxAllowedDiff = this.deviation.absoluteFor(Math.min(msrdPeak.getMass(), predPeak.getMass()));

            // Check if msrdPeak and predPeak are matching
            if(absDiff <= maxAllowedDiff){
                matchedPeaks.put(i,j);
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
