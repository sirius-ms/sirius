package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Mass deviation setting for MS1 spectra. Mass Deviations are always written as "X ppm (Y Da)" with X and Y
 * are numerical values. The ppm is a relative measure (parts per million), Da is an absolute measure. For each mass, the
 * maximum of relative and absolute is used.
 */
public final class MS1MassDeviation extends MassDeviation {

    /**
     * @param allowedMassDeviation maximum allowed mass deviation. Only molecular formulas within this mass window are considered.
     * @param standardMassDeviation expected mass deviation of the instrument. Is used for the scoring.
     * @param massDifferenceDeviation expected mass deviation of the instrument for two close peaks or for recalibrated spectra. Should be smaller than the standard mass deviation
     */
    public MS1MassDeviation(@NotNull Deviation allowedMassDeviation, @NotNull Deviation standardMassDeviation, @NotNull Deviation massDifferenceDeviation) {
        super(allowedMassDeviation, standardMassDeviation, massDifferenceDeviation);
    }

    @DefaultInstanceProvider
    public static MS1MassDeviation newInstance(@DefaultProperty(propertyKey = "allowedMassDeviation") Deviation allowedMassDeviation, @DefaultProperty(propertyKey = "standardMassDeviation") Deviation standardMassDeviation, @DefaultProperty(propertyKey = "massDifferenceDeviation") Deviation massDifferenceDeviation) {
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
