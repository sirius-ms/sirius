package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;

public interface LoadDialog {
	
	void newCollisionEnergy(CompactSpectrum sp);

	void ionizationChanged(PrecursorIonType ionization);
	
	void spectraAdded(CompactSpectrum sp);
	
	void spectraRemoved(CompactSpectrum sp);
	
	void msLevelChanged(CompactSpectrum sp);
	
	void addLoadDialogListener(LoadDialogListener ldl);
	
	void showDialog();
	
	void experimentNameChanged(String name);

	void parentMassChanged(double newMz);
}
