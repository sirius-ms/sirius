package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

@DefaultProperty(propertyParent = "NumberOfCandidates")
public class NumberOfCandidates implements Ms2ExperimentAnnotation {
    public static final NumberOfCandidates ZERO = new NumberOfCandidates(0);
    public static final NumberOfCandidates MIN_VALUE = new NumberOfCandidates(Integer.MIN_VALUE);
    public static final NumberOfCandidates MAX_VALUE = new NumberOfCandidates(Integer.MAX_VALUE);
    public static final NumberOfCandidates ONE = new NumberOfCandidates(1);


    public int value;

    public NumberOfCandidates() {

    }

    public NumberOfCandidates(int value) {
        this.value = value;
    }
}
