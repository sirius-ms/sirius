package de.unijena.bioinf.spectraldb.entities;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;

public interface ReferenceSpectrum {

    long getUuid();

    ReferenceLibrarySpectrum getQuerySpectrum();

    String getLibraryName();

    String getSmiles();

    MolecularFormula getFormula();

    double getExactMass();

    double getPrecursorMz();

    PrecursorIonType getPrecursorIonType();

    String getCandidateInChiKey();

    String getName();

}
