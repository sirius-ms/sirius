package de.unijena.bioinf.ChemistryBase.chem;

public class Ion extends Ionization{
	
	private int charge;
	private MolecularFormula molecule;
	
	public static final double ELECTRON_MASS = 0.0005485299999998805d;
	
	public Ion(MolecularFormula molecule, int charge){
		this.charge = charge;
		this.molecule = molecule;
	}

	@Override
	public double getMass() {
		return molecule.getMass() - charge*ELECTRON_MASS;
	}

	@Override
	public int getCharge() {
		return charge;
	}

	@Override
	public String getFormula() {
		return this.molecule.toString();
	}

}
