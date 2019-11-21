package de.unijena.bioinf.ms.gui.ms_viewer.data;


public class DefaultMolecularFormulaInformation implements MolecularFormulaInformation{
	
	private String formulaString;
	private double mass;
	private PeakInformation peak;
	
	private boolean useFormula;
	
	public DefaultMolecularFormulaInformation(){
		this("",0,true,null);
	}
	
	public DefaultMolecularFormulaInformation(String formula, double mass, boolean useFormula,PeakInformation peak){
		this.formulaString = formula;
		this.mass = mass;
		this.useFormula = useFormula;
		this.peak = peak;
	}
	
	public void setPeak(PeakInformation peak){
		this.peak = peak;
	}
	
	public void setMass(double mass){
		this.mass = mass;
	}
	
	public void setMolecularFormulaString(String formula){
		this.formulaString = formula;
	}

	@Override
	public String getMolecularFormula() {
		return formulaString;
	}

	@Override
	public double getMass() {
		return mass;
	}

	@Override
	public void useFormula(boolean use) {
		this.useFormula = use;
	}

	@Override
	public boolean formulaUsed() {
		return this.useFormula;
	}
	
	public String toString(){
		return this.formulaString+ " - "+this.mass;
	}

	@Override
	public PeakInformation getPeak() {
		return this.peak;
	}

}
