package de.unijena.bioinf.spectraldb.entities;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;

public interface ReferenceSpectrum {

    ReferenceLibrarySpectrum getQuerySpectrum();

    String getLibraryName();

    String getSmiles();

    MolecularFormula getFormula();

    double getExactMass();

    double getPrecursorMz();

    PrecursorIonType getPrecursorIonType();

    String getCandidateInChiKey();

}
