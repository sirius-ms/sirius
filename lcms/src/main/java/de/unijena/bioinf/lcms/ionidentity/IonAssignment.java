package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

class IonAssignment {

    final PrecursorIonType[] ionTypes;
    final double[] probabilities;

    public IonAssignment(PrecursorIonType[] ionTypes, double[] probabilities) {
        this.ionTypes = ionTypes;
        this.probabilities = probabilities;
    }

    public static IonAssignment uniform(Set<PrecursorIonType> possibleIonTypes) {
        final PrecursorIonType[] types = possibleIonTypes.toArray(PrecursorIonType[]::new);
        final double[] probs = new double[types.length];
        Arrays.fill(probs, 1.0f/probs.length);
        return new IonAssignment(types,probs);
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("{");
        for (int k=0; k < probabilities.length; ++k) {
            buf.append(String.format(Locale.US, "%s (%.1f %%)",ionTypes[k].toString(), probabilities[k]*100 ));
            if (k < buf.length()-1) buf.append(",\t");
        }
        buf.append("}");
        return buf.toString();
    }
}
