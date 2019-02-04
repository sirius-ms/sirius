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
    private static final SimpleSpectrum EMPTY_SPECTRUM = new SimpleSpectrum(new double[0], new double[0]);
    private static final Ms1IsotopePattern EMPTY_PATTERN = new Ms1IsotopePattern(EMPTY_SPECTRUM, 0d);

    private final SimpleSpectrum spectrum;
    private final double score;

    public static Ms1IsotopePattern none() {
        return EMPTY_PATTERN;
    }

    public Ms1IsotopePattern(Spectrum<? extends Peak> ms, double score) {
        this.spectrum = (ms.size()==0) ? EMPTY_SPECTRUM : new SimpleSpectrum(ms);
        this.score = score;
    }

    public Ms1IsotopePattern(Peak[] peaks, double score) {
        this.spectrum = peaks.length==0 ? EMPTY_SPECTRUM : Spectrums.from(Arrays.asList(peaks));
        this.score = score;
    }

    public boolean isEmpty() {
        return spectrum.size()==0;
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
