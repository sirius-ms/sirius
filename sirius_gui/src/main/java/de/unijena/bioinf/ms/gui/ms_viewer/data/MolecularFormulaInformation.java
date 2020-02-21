package de.unijena.bioinf.ms.gui.ms_viewer.data;


public interface MolecularFormulaInformation {
	
	String getMolecularFormula();
	
	double getMass();
	
	void useFormula(boolean use);
	
	boolean formulaUsed();
	
	PeakInformation getPeak();

}
