package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

public class MS2MassDeviation extends MassDeviation {
    public static final MS2MassDeviation DEFAULT =
            PropertyManager.DEFAULTS.createInstanceWithDefaults(MS2MassDeviation.class);

    public MS2MassDeviation(@NotNull Deviation allowedMassDeviation, @NotNull Deviation standardMassDeviation, @NotNull Deviation massDifferenceDeviation) {
        super(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @DefaultInstanceProvider
    public static MS2MassDeviation newInstance(@DefaultProperty Deviation allowedMassDeviation, @DefaultProperty Deviation standardMassDeviation) {
        return new MS2MassDeviation(allowedMassDeviation, standardMassDeviation, Deviation.NULL_DEVIATION);
    }
}
