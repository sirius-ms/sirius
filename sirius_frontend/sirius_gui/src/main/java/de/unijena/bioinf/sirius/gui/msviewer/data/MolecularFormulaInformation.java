package de.unijena.bioinf.sirius.gui.msviewer.data;


public interface MolecularFormulaInformation {
	
	public String getMolecularFormula();
	
	public double getMass();
	
	public void useFormula(boolean use);
	
	public boolean formulaUsed();
	
	public PeakInformation getPeak();

}
