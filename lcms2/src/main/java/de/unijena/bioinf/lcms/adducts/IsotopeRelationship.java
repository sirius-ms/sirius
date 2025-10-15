package de.unijena.bioinf.lcms.adducts;

public class IsotopeRelationship implements KnownMassDelta{

    private int isotopicShift;

    public IsotopeRelationship(int isotopicShift) {
        this.isotopicShift = isotopicShift;
    }

    public int getIsotopicShift() {
        return isotopicShift;
    }

    @Override
    public boolean isCompatible(IonType left, IonType right) {
        return true;
    }
}
