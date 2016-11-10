package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public enum Ionization {
	
	MPlusH("[M+H]+"), MPlus("[M]+"), MMinus("[M]-"), MPlusNa("[M+Na]+"), MMinusH("[M-H]-"), UnknownPlus("Unknown Positive"), UnknownNegative("Unknown Negative");

	private final String name;

	public static Ionization byName(String name) {
		for (Ionization i : values()) {
			if (i.name.equals(name)) return i;
		}
		return null;
	}

	private Ionization(String name) {
		this.name = name;
	}

	public PrecursorIonType toRealIonization() {
		if (this == UnknownPlus) return PrecursorIonType.unknown(1);
        if (this == UnknownNegative) return PrecursorIonType.unknown(-1);
        return PrecursorIonType.getPrecursorIonType(name);
	}

    @Override
    public String toString() {
        return name;
    }

    public boolean isUnknown() {
        return this==UnknownNegative || this==UnknownPlus;
    }

    public static Ionization fromSirius(PrecursorIonType ion) {
        if (!ion.isIonizationUnknown()) {
            final String name = ion.toString().replaceAll("\\s+","");
            final Ionization i = byName(name);
            if (i!=null) return i;
        }
        if (ion.getCharge()>0) return UnknownPlus;
        else return UnknownNegative;
    }
}
