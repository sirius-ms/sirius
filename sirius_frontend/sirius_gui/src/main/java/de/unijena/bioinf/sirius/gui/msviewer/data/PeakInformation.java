package de.unijena.bioinf.sirius.gui.msviewer.data;

import java.util.List;


public interface PeakInformation {
	
	double getMass();
	
	double getRelativeIntensity();
	
	double getAbsoluteIntensity();
	
	double getSignalToNoise();
	
	boolean isIsotope();
	
	List<MolecularFormulaInformation> getDecompositions();

}
