package de.unijena.bioinf.ChemistryBase.chem;

/**
 * Ionization mode in which the adduct is unknown and only the charge is known.
 * This is the default mode as long as the user don't give an ionization mode as input.
 */
public class Charge extends Ionization {
    /*
    it seems that Na - Na+ ~= H - H+ ~=
     */
    // source: Mohr, Peter J. and Taylor, Barry N. and Newell, David B., Rev. Mod. Phys. 2012.
	public final static double ELECTRON_MASS =   0.00054857990946d;
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
		return -charge * ELECTRON_MASS;
	}

	@Override
	public int getCharge() {
		return charge;
	}

	@Override
	public String getName() {
		return charge > 0 ? "[M+?]+" : (charge < 0 ? "[M+?]-" : "[M+?]"); 
	}
	
	

}
