package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;

import java.util.List;

public interface LoadDialog {

    void newCollisionEnergy(SpectrumContainer sp);

    void msLevelChanged(SpectrumContainer sp);

    void addLoadDialogListener(LoadDialogListener ldl);

    void showDialog();

    void experimentNameChanged(String name);

    String getExperimentName();

    double getParentMass();

    void ionizationChanged(PrecursorIonType ionization);


    PrecursorIonType getIonization();
}
