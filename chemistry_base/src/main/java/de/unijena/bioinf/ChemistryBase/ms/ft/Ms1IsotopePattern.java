package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * Isotope pattern coming from MS1
 * TODO: workaround. We have to clean that up
 */
public class Ms1IsotopePattern {

    private Peak[] peaks;
    private double score;

    public Ms1IsotopePattern(Spectrum<? extends Peak> ms, double score) {
        this.peaks = Spectrums.copyPeakList(ms).toArray(new Peak[ms.size()]);
        this.score = score;
    }

    public Ms1IsotopePattern(Peak[] peaks, double score) {
        this.peaks = peaks;
        this.score = score;
    }


    public Peak[] getPeaks() {
        return peaks;
    }

    public double getScore() {
        return score;
    }
}
