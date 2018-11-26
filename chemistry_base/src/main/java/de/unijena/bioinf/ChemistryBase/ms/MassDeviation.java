package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

public abstract class MassDeviation implements Ms2ExperimentAnnotation {

    public Deviation allowedMassDeviation;
    public Deviation standardMassDeviation;
    public Deviation massDifferenceDeviation;

    public MassDeviation(@NotNull Deviation allowedMassDeviation, @NotNull Deviation standardMassDeviation, @NotNull Deviation massDifferenceDeviation) {
        this.allowedMassDeviation = allowedMassDeviation;
        this.standardMassDeviation = standardMassDeviation;
        this.massDifferenceDeviation = massDifferenceDeviation;
    }
}
