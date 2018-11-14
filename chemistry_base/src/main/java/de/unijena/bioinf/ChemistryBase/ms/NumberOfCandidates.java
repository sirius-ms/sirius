package de.unijena.bioinf.ChemistryBase.ms;

public class NumberOfCandidates implements Ms2ExperimentAnnotation {
    public final int value;

    public NumberOfCandidates(int value) {
        this.value = value;
    }
}
