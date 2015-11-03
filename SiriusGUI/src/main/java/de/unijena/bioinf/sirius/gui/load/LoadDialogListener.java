package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;

import java.io.File;
import java.util.List;

public interface LoadDialogListener {

	public void addSpectra();
	
	public void addSpectra(List<File> files);
	
	public void removeSpectrum(CompactSpectrum sp);
	
	public void abortProcess();
	
	public void completeProcess();
	
	public void changeCollisionEnergy(CompactSpectrum sp);

	public void setIonization(Ionization ionization);
	
	public void changeMSLevel(CompactSpectrum sp, int msLevel);
	
	public void experimentNameChanged(String name);

}
