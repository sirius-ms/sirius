package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

public final class MS1MassDeviation extends MassDeviation {

    public MS1MassDeviation(@NotNull Deviation allowedMassDeviation, @NotNull Deviation standardMassDeviation, @NotNull Deviation massDifferenceDeviation) {
        super(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @DefaultInstanceProvider
    public static MS1MassDeviation newInstance(@DefaultProperty Deviation allowedMassDeviation, @DefaultProperty Deviation standardMassDeviation, @DefaultProperty Deviation massDifferenceDeviation) {
        return new MS1MassDeviation(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @Override
    public MS1MassDeviation withAllowedMassDeviation(Deviation allowedMassDeviation) {
        return new MS1MassDeviation(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @Override
    public MS1MassDeviation withStandardMassDeviation(Deviation standardMassDeviation) {
        return new MS1MassDeviation(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @Override
    public MS1MassDeviation withMassDifferenceDeviation(Deviation massDifferenceDeviation) {
        return new MS1MassDeviation(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }
}
