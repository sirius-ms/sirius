package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class SpectralLibraryHit {

    private final LibrarySpectrum libraryHit;
    private final double cosine;
    private final int sharedPeaks;

    public SpectralLibraryHit(LibrarySpectrum libraryHit, double cosine, int numberOfSharedPeaks) {
        this.libraryHit = libraryHit;

        this.cosine = cosine;
        this.sharedPeaks = numberOfSharedPeaks;
    }

    public LibrarySpectrum getLibraryHit() {
        return this.libraryHit;
    }

    public MolecularFormula getMolecularFormula() {
        return this.libraryHit.getMolecularFormula();
    }

    public double getCosine() {
        return this.cosine;
    }

    public PrecursorIonType getIonType() {
        return this.libraryHit.getIonType();
    }

    public int getNumberOfSharedPeaks() {
        return this.sharedPeaks;
    }

}
