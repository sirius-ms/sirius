package de.unijena.bioinf.ChemistryBase.ms.lcms;

import javax.annotation.Nonnull;

/**
 * A mass trace of an ion consists of the mass traces of its isotope peaks
 */
public class IonTrace {

    /**
     * traces of the isotopes. The monoisotopic peak has index 0.
     */
    @Nonnull protected final Trace[] isotopes;

    public IonTrace(@Nonnull Trace[] isotopes) {
        this.isotopes = isotopes;
    }

    @Nonnull
    public Trace[] getIsotopes() {
        return isotopes;
    }
}
