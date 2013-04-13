package de.unijena.bioinf.ChemistryBase.chem;

/**
 * Ionization mode in which the adduct is unknown and only the charge is known.
 * This is the default mode as long as the user don't give an ionization mode as input.
 */
public class Charge extends Ionization {
    /*
    it seems that Na - Na+ ~= H - H+ ~=
     */
	public final static double PROTON_MASS = 0.0005485299999998805d;
	private final int charge;
	
	public Charge(int charge) {
		this.charge = charge;
	}

    /**
     * [M + H+] ~= [M + H] - [H - H+] => "Ion mass" = -charge * (H-H+) = difference between molecule+H and molecule+ion
     *
     * @return
     */
	@Override
	public double getMass() {
		return -charge * PROTON_MASS;
	}

	@Override
	public int getCharge() {
		return charge;
	}

	@Override
	public String getFormula() {
		return charge > 0 ? "[M+?]+" : (charge < 0 ? "[M+?]-" : "[M+?]"); 
	}
	
	

}
