package de.unijena.bioinf.ms.gui.sirius.msviewer.data;


public interface MolecularFormulaInformation {
	
	String getMolecularFormula();
	
	double getMass();
	
	void useFormula(boolean use);
	
	boolean formulaUsed();
	
	PeakInformation getPeak();

}
