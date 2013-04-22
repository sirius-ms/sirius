package de.unijena.bioinf.ChemistryBase.chem;

/**
 * Ionization mode in which a small ion (adduct) is attached to the molecule ([M+ion]), an ion is removed from the
 * molecule ([M-ion]) or the molecule itself is an ion ([M]).
 *
 */
public class Adduct extends Ionization {
	private final double mass;
	private final int charge;
	private final String formula;
	private final MolecularFormula molecularFormula;
	
	public Adduct(int charge, String name, MolecularFormula formula) {
		this(formula.getMass(), charge, name, formula);
	}
	
	public Adduct(double mass, int charge, String name, MolecularFormula formula) {
		this.mass = mass;
		this.charge = charge;
		if (formula == null) throw new NullPointerException("Expect non-null formula");
		this.formula = name;
		this.molecularFormula = formula;
	}
	
	public Adduct(double mass, int charge, String formula) {
		this.mass = mass;
		this.charge = charge;
		if (formula == null) throw new NullPointerException("Expect non-null formula");
		this.formula = formula;
		this.molecularFormula = null;
	}
	
	/**
     * if the adduct is known, return its molecular formula (may be negative). Protons and electrons should not be added (because they
     * are no atoms)! In general: add only atoms to the formula which have isotopic species, because this method is
     * used for isotopic pattern generation of a formula with the given ionization mode.
     */
    public MolecularFormula getAtoms() {
    	return molecularFormula;
    }

	@Override
	public double getMass() {
		return mass;
	}

	@Override
	public int getCharge() {
		return charge;
	}

	@Override
	public String getFormula() {
		return formula;
	}

    @Override
    public String toString() {
        return formula;
    }
}
