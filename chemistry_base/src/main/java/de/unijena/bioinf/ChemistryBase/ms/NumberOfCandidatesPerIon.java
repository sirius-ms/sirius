package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/*
 * use this parameter if you want to force to report at least
 * numberOfResultsToKeepPerIonization results per ionization.
 * if <= 0, this parameter will have no effect and just the top
 * numberOfResultsToKeep results will be reported.
 * */
@DefaultProperty
public class NumberOfCandidatesPerIon implements Ms2ExperimentAnnotation {
    public static final NumberOfCandidatesPerIon ZERO = new NumberOfCandidatesPerIon(0);
    public static final NumberOfCandidatesPerIon MIN_VALUE = new NumberOfCandidatesPerIon(Integer.MIN_VALUE);
    public static final NumberOfCandidatesPerIon MAX_VALUE = new NumberOfCandidatesPerIon(Integer.MAX_VALUE);
    public static final NumberOfCandidatesPerIon ONE = new NumberOfCandidatesPerIon(1);

    public int value;

    public NumberOfCandidatesPerIon() {

    }

    public NumberOfCandidatesPerIon(int value) {
        this.value = value;
    }
}
