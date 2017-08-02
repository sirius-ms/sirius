package de.unijena.bioinf.sirius.gui.msviewer.data;


public interface MolecularFormulaInformation {
	
	String getMolecularFormula();
	
	double getMass();
	
	void useFormula(boolean use);
	
	boolean formulaUsed();
	
	PeakInformation getPeak();

}
