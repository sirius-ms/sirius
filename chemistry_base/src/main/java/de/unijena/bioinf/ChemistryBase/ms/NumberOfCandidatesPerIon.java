package de.unijena.bioinf.ChemistryBase.ms;

public class NumberOfCandidatesPerIon implements Ms2ExperimentAnnotation {
    public final int value;

    public NumberOfCandidatesPerIon(int value) {
        this.value = value;
    }
}
