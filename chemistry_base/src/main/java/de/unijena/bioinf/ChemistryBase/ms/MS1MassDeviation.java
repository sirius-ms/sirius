package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class MS1MassDeviation extends MassDeviation {

    public MS1MassDeviation(Deviation allowedMassDeviation, Deviation standardMassDeviation) {
        super(allowedMassDeviation, standardMassDeviation);
    }

    @DefaultInstanceProvider
    public static MS1MassDeviation newInstance(@DefaultProperty Deviation allowedMassDeviation, @DefaultProperty Deviation standardMassDeviation) {
        return new MS1MassDeviation(allowedMassDeviation, standardMassDeviation);
    }
}
