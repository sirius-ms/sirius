package de.unijena.bioinf.myxo.gui.msviewer.data;

import java.util.List;


public interface PeakInformation {
	
	@SuppressWarnings("unused")
	public double getMass();
	
	@SuppressWarnings("unused")
	public double getRelativeIntensity();
	
	@SuppressWarnings("unused")
	public double getAbsoluteIntensity();
	
	@SuppressWarnings("unused")
	public double getSignalToNoise();
	
	@SuppressWarnings("unused")
	public boolean isIsotope();
	
	@SuppressWarnings("unused")
	public List<MolecularFormulaInformation> getDecompositions();

}
