package de.unijena.bioinf.spectraldb.entities;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.spectraldb.SpectrumType;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;

public interface ReferenceSpectrum {

    public ReferenceLibrarySpectrum getQuerySpectrum();

    public String getLibraryName();

    public String getSmiles();

    public MolecularFormula getFormula();

    public double getExactMass();

    public double getPrecursorMz();

    public PrecursorIonType getPrecursorIonType();

    public String getCandidateInChiKey();

}
