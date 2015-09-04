package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.myxo.structure.CompactSpectrum;

public interface LoadDialogListener {

	public void addSpectra();
	
	public void removeSpectrum(CompactSpectrum sp);
	
	public void abortProcess();
	
	public void completeProcess();
	
	public void changeCollisionEnergy(CompactSpectrum sp);
	
	public void changeMSLevel(CompactSpectrum sp, int msLevel);
	
	public void experimentNameChanged(String name);

}
