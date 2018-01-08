package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;

public interface LoadDialog {

    void newCollisionEnergy(SpectrumContainer sp);

    void msLevelChanged(SpectrumContainer sp);

    void addLoadDialogListener(LoadDialogListener ldl);

    void showDialog();

    void experimentNameChanged(String name);

    String getExperimentName();

    void parentMassChanged(double newMz);

    double getParentMass();

    void ionizationChanged(PrecursorIonType ionization);

    PrecursorIonType getIonization();
}
