package de.unijena.bioinf.ChemistryBase.chem;

public class Ion extends Ionization{
	
	private int charge;
	private MolecularFormula molecule;
	
	public static final double ELECTRON_MASS = 0.00054857990946d;
	// siehe http://physics.nist.gov/cgi-bin/cuu/Value?meu
	
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
