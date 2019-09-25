package de.unijena.bioinf.ms.gui.sirius.msviewer.data;

import java.util.List;


public interface PeakInformation {
	
	double getMass();
	
	double getRelativeIntensity();
	
	double getAbsoluteIntensity();

	boolean isIsotope();
	
	List<MolecularFormulaInformation> getDecompositions();

}
