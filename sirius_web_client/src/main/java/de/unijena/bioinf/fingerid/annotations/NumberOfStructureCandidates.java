package de.unijena.bioinf.fingerid.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty
public class NumberOfStructureCandidates implements Ms2ExperimentAnnotation {
    public static final NumberOfStructureCandidates ZERO = new NumberOfStructureCandidates(0);
    public static final NumberOfStructureCandidates MIN_VALUE = new NumberOfStructureCandidates(Integer.MIN_VALUE);
    public static final NumberOfStructureCandidates MAX_VALUE = new NumberOfStructureCandidates(Integer.MAX_VALUE);
    public static final NumberOfStructureCandidates ONE = new NumberOfStructureCandidates(1);

    public final int value;

    private NumberOfStructureCandidates() {
        this(0);
    }
    public NumberOfStructureCandidates(int value) {
        this.value = value;
    }
}
