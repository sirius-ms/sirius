package de.unijena.bioinf.ChemistryBase.chem;

/**
 * The IonType is an arbitrary modification of the molecular formula of the precursor ion
 *
 * it includes in-source fragmentations and adducts but separates them from the ion mode.
 *
 * For example, [M+NH4]+ consists of the ionization H+ and the adduct NH3.
 * However, [M+Na]+ consists of the ionization Na+ but has no adduct
 * [M-H2O+H]+ consists of the ionization H+ and the in-source fragmentation H2O
 *
 * Besides the chemistry background, IonType works as follows:
 * - every modification that has to be applied to the precursor ion AND all of its fragments go into ionization
 * - every modification that only apply to the precursor (e.g. in-source fragmentation) goes into modification
 * - every modification that apply to the precursor but might get lost in the fragments (e.g. adducts) goes into modification
 */
public class PrecursorIonType {

    private final Ionization ionization;
    private final MolecularFormula inSourceFragmentation;
    private final MolecularFormula adduct;
    private final MolecularFormula modification;
    private final String name;

    public static PrecursorIonType getPrecursorIonType(String name) {
        return PeriodicTable.getInstance().ionByName(name);
    }

    public static PrecursorIonType getPrecursorIonType(Ionization ion) {
        return PeriodicTable.getInstance().getPrecursorIonTypeFromIonization(ion);
    }

    public static PrecursorIonType unknown(int charge) {
        return PeriodicTable.getInstance().getUnknownPrecursorIonType(charge);
    }

    PrecursorIonType(Ionization ion, MolecularFormula insource, MolecularFormula adduct) {
        this.ionization = ion;
        this.inSourceFragmentation = insource==null ? MolecularFormula.emptyFormula() : insource;
        this.adduct = adduct==null ? MolecularFormula.emptyFormula() : adduct;
        this.modification = this.adduct.subtract(this.inSourceFragmentation);
        this.name = formatToString();
    }

    public boolean equals(PrecursorIonType other) {
        if (other==null) return false;
        return this.ionization.equals(other.ionization) && this.modification.equals(other.modification);
    }

    public boolean equals(Object other) {
        if (other==null) return false;
        if (other instanceof PrecursorIonType) {
            return equals((PrecursorIonType)other);
        } else return false;
    }

    public PrecursorIonType withoutAdduct() {
        return new PrecursorIonType(getIonization(), inSourceFragmentation, MolecularFormula.emptyFormula());
    }

    public PrecursorIonType withoutInsource() {
        return new PrecursorIonType(getIonization(), MolecularFormula.emptyFormula(), adduct);
    }

    @Override
    public int hashCode() {
        return 31 * ionization.hashCode() + modification.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    private String formatToString() {
        final StringBuilder buf = new StringBuilder(128);
        buf.append("[M");
        MolecularFormula ion = MolecularFormula.emptyFormula();
        if (ionization.getAtoms()!=null) ion = ion.add(ionization.getAtoms());
        final boolean isH = ion.numberOfHydrogens() == ion.atomCount();
        final boolean isNeg = ion.atomCount() < 0;
        MolecularFormula subtractions = MolecularFormula.emptyFormula();
        subtractions = subtractions.add(inSourceFragmentation);
        if (isH && isNeg) subtractions = subtractions.subtract(ion);
        MolecularFormula additions = MolecularFormula.emptyFormula();
        additions=additions.add(adduct);
        if (isH && !isNeg) additions=additions.add(ion);
        if (subtractions.atomCount()>0) {
            buf.append(" - ");
            buf.append(subtractions);
        }
        if (additions.atomCount() > 0) {
            buf.append(" + ");
            buf.append(additions);
        }
        if (!isH) {
            if (isNeg) buf.append(" - ").append(ion.negate().toString());
            else buf.append(" + ").append(ion);
        }
        buf.append("]");
        if (ionization.getCharge() > 0) {
            if (ionization.getCharge() > 1) buf.append(ionization.getCharge());
            buf.append("+");
        } else {
            if (ionization.getCharge() < -1) buf.append(-ionization.getCharge());
            buf.append("-");
        }
        return buf.toString();
    }

    public boolean isIonizationUnknown() {
        return ionization instanceof Charge;
    }

    public int getCharge() {
        return ionization.getCharge();
    }

    public MolecularFormula neutralMoleculeToMeasuredNeutralMolecule(MolecularFormula neutral) {
        return neutral.subtract(inSourceFragmentation).add(adduct);
    }

    public MolecularFormula measuredNeutralMoleculeToNeutralMolecule(MolecularFormula measured) {
        return measured.add(inSourceFragmentation).subtract(adduct);
    }


    public MolecularFormula precursorIonToNeutralMolecule(MolecularFormula precursor) {
        return precursor.subtract(modification).subtract(ionization.getAtoms());
    }

    public MolecularFormula neutralMoleculeToPrecursorIon(MolecularFormula formula) {
        return formula.add(modification).add(ionization.getAtoms());
    }

    /**
     * @return the mass difference between the ion mass and the neutral mass including in-source fragmentation, adduct, and electron masses
     */
    public double getModificationMass() {
        return ionization.getMass() + modification.getMass();
    }

    public double subtractIonAndAdduct(double mz) {
        return ionization.subtractFromMass(mz - adduct.getMass());
    }

    public double precursorMassToNeutralMass(double mz) {
        return ionization.subtractFromMass(mz - modification.getMass());
    }

    public double neutralMassToPrecursorMass(double mz) {
        return ionization.addToMass(mz + modification.getMass());
    }





    /**
     * @return the sum of all modifications (adducts and in-source fragmentations)
     */
    public MolecularFormula getModification() {
        return modification;
    }

    public Ionization getIonization() {
        return ionization;
    }

    public MolecularFormula getInSourceFragmentation() {
        return inSourceFragmentation;
    }

    public MolecularFormula getAdduct() {
        return adduct;
    }

    public MolecularFormula getAdductAndIons() {
        return adduct.add(ionization.getAtoms());
    }
}
