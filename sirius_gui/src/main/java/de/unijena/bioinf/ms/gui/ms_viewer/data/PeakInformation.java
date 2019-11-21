package de.unijena.bioinf.ms.gui.ms_viewer.data;

import java.util.List;


public interface PeakInformation {
	
	double getMass();
	
	double getRelativeIntensity();
	
	double getAbsoluteIntensity();

	boolean isIsotope();
	
	List<MolecularFormulaInformation> getDecompositions();

}
