package de.unijena.bioinf.myxo.gui.msviewer.data;

import java.util.List;


public interface PeakInformation {
	
	@SuppressWarnings("unused")
    double getMass();
	
	@SuppressWarnings("unused")
    double getRelativeIntensity();
	
	@SuppressWarnings("unused")
    double getAbsoluteIntensity();
	
	@SuppressWarnings("unused")
    boolean isIsotope();
	
	@SuppressWarnings("unused")
    List<MolecularFormulaInformation> getDecompositions();

}
