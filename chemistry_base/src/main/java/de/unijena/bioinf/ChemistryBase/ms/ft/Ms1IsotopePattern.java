package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.util.Arrays;

/**
 * Isotope pattern coming from MS1
 * TODO: workaround. We have to clean that up
 */
public class Ms1IsotopePattern implements ProcessedInputAnnotation, TreeAnnotation {

    private SimpleSpectrum spectrum;
    private double score;

    public Ms1IsotopePattern(Spectrum<? extends Peak> ms, double score) {
        this.spectrum = new SimpleSpectrum(ms);
        this.score = score;
    }

    public Ms1IsotopePattern(Peak[] peaks, double score) {
        this.spectrum = Spectrums.from(Arrays.asList(peaks));
        this.score = score;
    }


    public Peak[] getPeaks() {
        return Spectrums.extractPeakList(spectrum).toArray(new Peak[0]);
    }

    public double getScore() {
        return score;
    }

    public SimpleSpectrum getSpectrum() {
        return spectrum;
    }
}
