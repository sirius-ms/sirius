package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.GibbsSampling.LibraryHitQuality;

public class LibraryHit {
    private final Ms2Experiment queryExperiment;
    private final MolecularFormula molecularFormula;
    private final String structure;
    private final double cosine;
    private final PrecursorIonType ionType;
    private final int sharedPeaks;
    private final LibraryHitQuality quality;
    private final double precursorMz;

    public LibraryHit(Ms2Experiment queryExperiment, MolecularFormula molecularFormula, String structure, PrecursorIonType ionType, double cosine, int sharedPeaks, LibraryHitQuality quality, double libPrecursorMz) {
        this.queryExperiment = queryExperiment;
        this.molecularFormula = molecularFormula;
        this.structure = structure;
        this.ionType = ionType;
        this.cosine = cosine;
        this.sharedPeaks = sharedPeaks;
        this.quality = quality;
        this.precursorMz = libPrecursorMz;
    }

    public Ms2Experiment getQueryExperiment() {
        return this.queryExperiment;
    }

    public MolecularFormula getMolecularFormula() {
        return this.molecularFormula;
    }

    public String getStructure() {
        return this.structure;
    }

    public double getCosine() {
        return this.cosine;
    }

    public PrecursorIonType getIonType() {
        return this.ionType;
    }

    public int getSharedPeaks() {
        return this.sharedPeaks;
    }

    public LibraryHitQuality getQuality() {
        return this.quality;
    }

    public double getPrecursorMz() {
        return precursorMz;
    }
}
