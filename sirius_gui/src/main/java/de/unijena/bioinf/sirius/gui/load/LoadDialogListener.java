package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.io.File;
import java.util.List;

public interface LoadDialogListener {

    void addSpectra();

    void addSpectra(List<File> files);

    void removeSpectrum(Spectrum<?> sp);

    void abortProcess();

    void completeProcess();

    void changeCollisionEnergy(Spectrum<?> sp);

    void setIonization(PrecursorIonType ionization);

    void changeMSLevel(Spectrum<?> sp, int msLevel);

    void experimentNameChanged(String name);

    void setParentMass(double mz);
}
