package de.unijena.bioinf.ChemistryBase.chem;

/**
 * EI Ionization mode. An electron is dislodged from the neutral molecule.
 * The getMass() is negative to make addToMass() and subtractFromMass() work as indicated.
 */
public class ElectronIonization extends Ionization {


    public ElectronIonization(){
    }

    @Override
    public double getMass() {
        return -Charge.ELECTRON_MASS;
    }

    @Override
    public int getCharge() {
        return 1;
    }

    @Override
    public String getName() {
        return "M+.";
    }
}
