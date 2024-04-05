package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class AdductRelationship implements KnownMassDelta{

    protected final KnownAdductType left, right;

    public AdductRelationship(KnownAdductType left, KnownAdductType right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean isCompatible(KnownAdductType left, KnownAdductType right) {
        return left.equals(this.left) && right.equals(this.right);
    }

    @Override
    public String toString() {
        return "AdductRelationship{" + left.toString() + " --> " + right.toString() + "}";
    }
}
