package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public enum CompoundCandidateChargeState {

    NEUTRAL_CHARGE(1),POSITIVE_CHARGE(2),NEGATIVE_CHARGE(4);

    public static CompoundCandidateChargeState getFromPrecursorIonType(PrecursorIonType ionType) {
        if (ionType.isIntrinsicalCharged()) {
            if (ionType.getCharge()>0) return POSITIVE_CHARGE;
            else return NEGATIVE_CHARGE;
        } else return NEUTRAL_CHARGE;
    }

    private int value;

    private CompoundCandidateChargeState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
