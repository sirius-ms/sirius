package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

public final class MS1MassDeviation extends MassDeviation {
    public static final MS1MassDeviation DEFAULT() {
        return PropertyManager.DEFAULTS.createInstanceWithDefaults(MS1MassDeviation.class);
    }

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
