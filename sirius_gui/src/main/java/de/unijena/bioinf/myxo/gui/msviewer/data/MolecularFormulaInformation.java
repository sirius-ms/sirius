package de.unijena.bioinf.myxo.gui.msviewer.data;


@SuppressWarnings("unused")
public interface MolecularFormulaInformation {
	
	@SuppressWarnings("unused")
    String getMolecularFormula();
	
	@SuppressWarnings("unused")
    double getMass();
	
	@SuppressWarnings("unused")
    void useFormula(boolean use);
	
	@SuppressWarnings("unused")
    boolean formulaUsed();
	
	@SuppressWarnings("unused")
    PeakInformation getPeak();

}
