package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class MS2MassDeviation extends MassDeviation {

    public MS2MassDeviation(Deviation allowedMassDeviation, Deviation standardMassDeviation) {
        super(allowedMassDeviation, standardMassDeviation);
    }

    @DefaultInstanceProvider
    public static MS2MassDeviation newInstance(@DefaultProperty Deviation allowedMassDeviation, @DefaultProperty Deviation standardMassDeviation) {
        return new MS2MassDeviation(allowedMassDeviation, standardMassDeviation);
    }
}
