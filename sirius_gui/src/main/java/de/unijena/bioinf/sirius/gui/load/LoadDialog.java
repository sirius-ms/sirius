package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;

import javax.swing.*;

public interface LoadDialog {

    void newCollisionEnergy(SpectrumContainer sp);

//    void spectraAdded(Spectrum<?> sp);

//    void spectraRemoved(Spectrum<?> sp);

    void msLevelChanged(SpectrumContainer sp);

    void addLoadDialogListener(LoadDialogListener ldl);

    void showDialog();

    void experimentNameChanged(String name);

    String getExperimentName();

    void parentMassChanged(double newMz);

    double getParentMass();

    void ionizationChanged(PrecursorIonType ionization);

    PrecursorIonType getIonization();

    DefaultListModel<SpectrumContainer> getSpectra();
}
