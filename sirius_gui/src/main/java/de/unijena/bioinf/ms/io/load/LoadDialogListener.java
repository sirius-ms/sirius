package de.unijena.bioinf.ms.io.load;

import de.unijena.bioinf.ms.gui.sirius.SpectrumContainer;

import java.io.File;
import java.util.List;

public interface LoadDialogListener {

    void addSpectra();

    void addSpectra(List<File> files);

    void removeSpectra(List<SpectrumContainer> sps);

    void changeCollisionEnergy(SpectrumContainer sp);

    void changeMSLevel(SpectrumContainer sp, int msLevel);

    void completeProcess();

    default void abortProcess() {
    }
}
