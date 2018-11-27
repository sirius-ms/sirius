package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.jetbrains.annotations.NotNull;

public abstract class MassDeviation implements Ms2ExperimentAnnotation {

    public final Deviation allowedMassDeviation;
    public final Deviation standardMassDeviation;
    public final Deviation massDifferenceDeviation;

    public MassDeviation(@NotNull Deviation allowedMassDeviation, @NotNull Deviation standardMassDeviation, @NotNull Deviation massDifferenceDeviation) {
        this.allowedMassDeviation = allowedMassDeviation;
        this.standardMassDeviation = standardMassDeviation;
        this.massDifferenceDeviation = massDifferenceDeviation;
    }

    public abstract <T extends MassDeviation> T withAllowedMassDeviation(Deviation allowedMassDeviation);
    public abstract <T extends MassDeviation> T withStandardMassDeviation(Deviation standardMassDeviation);
    public abstract <T extends MassDeviation> T withMassDifferenceDeviation(Deviation massDifferenceDeviation);
}
