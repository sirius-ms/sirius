package de.unijena.bioinf.ms.io.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.sirius.SpectrumContainer;

public interface LoadDialog {

    void newCollisionEnergy(SpectrumContainer sp);

    void msLevelChanged(SpectrumContainer sp);

    void addLoadDialogListener(LoadDialogListener ldl);

    void showDialog();

    void experimentNameChanged(String name);

    String getExperimentName();

    double getParentMass();

    void setParentMass(double ionMass);

    void ionizationChanged(PrecursorIonType ionization);


    PrecursorIonType getIonization();
}
