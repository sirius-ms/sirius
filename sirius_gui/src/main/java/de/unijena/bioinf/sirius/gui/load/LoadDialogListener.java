package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;

import java.io.File;
import java.util.List;

public interface LoadDialogListener {

	void addSpectra();
	
	void addSpectra(List<File> files);
	
	void removeSpectrum(CompactSpectrum sp);
	
	void abortProcess();
	
	void completeProcess();
	
	void changeCollisionEnergy(CompactSpectrum sp);

	void setIonization(PrecursorIonType ionization);
	
	void changeMSLevel(CompactSpectrum sp, int msLevel);
	
	void experimentNameChanged(String name);

	void setParentmass(double mz);
}
