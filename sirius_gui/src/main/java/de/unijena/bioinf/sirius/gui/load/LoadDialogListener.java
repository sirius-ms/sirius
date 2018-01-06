package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;

import java.io.File;
import java.util.List;

public interface LoadDialogListener {

    void addSpectra();

    void addSpectra(List<File> files);

    void removeSpectrum(SpectrumContainer sp);


    void changeCollisionEnergy(SpectrumContainer sp);


    void changeMSLevel(SpectrumContainer sp, int msLevel);

    void completeProcess();

    default void abortProcess() {
    }
}
