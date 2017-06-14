package de.unijena.bioinf.myxo.gui.msviewer.data;


@SuppressWarnings("unused")
public interface MolecularFormulaInformation {
	
	@SuppressWarnings("unused")
	public String getMolecularFormula();
	
	@SuppressWarnings("unused")
	public double getMass();
	
	@SuppressWarnings("unused")
	public void useFormula(boolean use);
	
	@SuppressWarnings("unused")
	public boolean formulaUsed();
	
	@SuppressWarnings("unused")
	public PeakInformation getPeak();

}
