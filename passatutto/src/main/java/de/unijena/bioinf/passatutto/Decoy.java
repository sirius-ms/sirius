package de.unijena.bioinf.passatutto;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.sirius.ResultAnnotation;

public class Decoy implements ResultAnnotation {

    /**
     * decoy spectrum
     */
    protected final SimpleSpectrum decoySpectrum;

    /**
     * precursor mass
     */
    protected final double precursor;

    /**
     * decoy tree (if available)
     */
    protected final FTree decoyTree;

    public Decoy(SimpleSpectrum decoySpectrum, double precursor, FTree decoyTree) {
        this.decoySpectrum = decoySpectrum;
        this.precursor = precursor;
        this.decoyTree = decoyTree;
    }

    public SimpleSpectrum getDecoySpectrum() {
        return decoySpectrum;
    }

    public double getPrecursor() {
        return precursor;
    }

    public FTree getDecoyTree() {
        return decoyTree;
    }
}
