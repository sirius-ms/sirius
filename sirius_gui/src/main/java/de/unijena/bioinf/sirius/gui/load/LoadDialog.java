package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.Iterator;

public interface LoadDialog {

    void newCollisionEnergy(Spectrum<?> sp);

    void ionizationChanged(PrecursorIonType ionization);

    void spectraAdded(Spectrum<?> sp);

    void spectraRemoved(Spectrum<?> sp);

    void msLevelChanged(Spectrum<?> sp);

    void addLoadDialogListener(LoadDialogListener ldl);

    void showDialog();

    void experimentNameChanged(String name);

    void parentMassChanged(double newMz);

    Iterator<Spectrum<?>> getSpectra();
}
