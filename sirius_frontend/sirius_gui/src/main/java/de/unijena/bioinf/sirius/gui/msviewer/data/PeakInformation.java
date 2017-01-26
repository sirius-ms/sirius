package de.unijena.bioinf.sirius.gui.msviewer.data;

import java.util.List;


public interface PeakInformation {
	
	public double getMass();
	
	public double getRelativeIntensity();
	
	public double getAbsoluteIntensity();
	
	public double getSignalToNoise();
	
	public boolean isIsotope();
	
	public List<MolecularFormulaInformation> getDecompositions();

}
