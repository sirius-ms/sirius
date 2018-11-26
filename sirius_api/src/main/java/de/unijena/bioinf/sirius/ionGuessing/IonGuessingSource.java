package de.unijena.bioinf.sirius.ionGuessing;

/**
 * the information source which was used to guess the compounds ionization.
 * NoSource: no useful MS1 spectrum was available
 * MergedMs1Spectrum: used merged MS1 spectrum
 * NormalMs1: merged MS1 was not available or only contained precursor peak (+ isotope peaks)
 */
public enum IonGuessingSource {
    NoSource, MergedMs1Spectrum, NormalMs1
}