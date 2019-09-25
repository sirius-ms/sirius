package de.unijena.bioinf.ChemistryBase.ms;


import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;

public class MergedMs1Spectrum implements ProcessedInputAnnotation {

    public final boolean peaksAreCorrelated;
    public final SimpleSpectrum mergedSpectrum;

    private final static MergedMs1Spectrum EMPTY = new MergedMs1Spectrum(false, new SimpleSpectrum(new double[0], new double[0]));

    public static MergedMs1Spectrum empty() {
        return EMPTY;
    }

    public MergedMs1Spectrum(boolean peaksAreCorrelated, SimpleSpectrum mergedSpectrum) {
        this.peaksAreCorrelated = peaksAreCorrelated;
        this.mergedSpectrum = mergedSpectrum;
    }

    public boolean isEmpty() {
        return mergedSpectrum.size()==0;
    }

    public boolean isCorrelated() {
        return peaksAreCorrelated;
    }
}
