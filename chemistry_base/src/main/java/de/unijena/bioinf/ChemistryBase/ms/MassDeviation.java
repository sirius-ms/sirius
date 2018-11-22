package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

public abstract class MassDeviation implements Ms2ExperimentAnnotation {

    public Deviation allowedMassDeviation;
    public Deviation standardMassDeviation;

    public MassDeviation(Deviation allowedMassDeviation, Deviation standardMassDeviation) {
        this.allowedMassDeviation = allowedMassDeviation;
        this.standardMassDeviation = standardMassDeviation;
    }
}
