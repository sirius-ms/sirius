package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class AdductRelationship implements KnownMassDelta{

    protected final PrecursorIonType left, right;

    public AdductRelationship(PrecursorIonType left, PrecursorIonType right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean isCompatible(IonType left, IonType right) {
        if (left instanceof IonType && right instanceof IonType) {
            return ((IonType) left).ionType.equals(this.left) && ((IonType) right).ionType.equals(this.right);
        } else return false;
    }

    public PrecursorIonType getLeft() {
        return left;
    }

    public PrecursorIonType getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "AdductRelationship{" + left.toString() + " --> " + right.toString() + "}";
    }
}
