package de.unijena.bioinf.sirius.gui.msviewer.data;

import java.util.List;


public interface PeakInformation {
	
	double getMass();
	
	double getRelativeIntensity();
	
	double getAbsoluteIntensity();

	boolean isIsotope();
	
	List<MolecularFormulaInformation> getDecompositions();

}
