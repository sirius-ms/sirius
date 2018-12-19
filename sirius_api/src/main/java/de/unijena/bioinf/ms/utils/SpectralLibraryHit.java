package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class SpectralLibraryHit {

    private final MolecularFormula estimatedFormula;
    private final LibrarySpectrum libraryHit;
    private final double cosine;
    private final int sharedPeaks;

    /**
     *
     * @param libraryHit
     * @param estimatedFormula might be different from library MF in case of mass difference to compound
     * @param cosine
     * @param numberOfSharedPeaks
     */
    public SpectralLibraryHit(LibrarySpectrum libraryHit, MolecularFormula estimatedFormula, double cosine, int numberOfSharedPeaks) {
        this.libraryHit = libraryHit;
        this.estimatedFormula = estimatedFormula;

        this.cosine = cosine;
        this.sharedPeaks = numberOfSharedPeaks;
    }

    public LibrarySpectrum getLibraryHit() {
        return this.libraryHit;
    }

    public MolecularFormula getMolecularFormula() {
        return estimatedFormula;
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
