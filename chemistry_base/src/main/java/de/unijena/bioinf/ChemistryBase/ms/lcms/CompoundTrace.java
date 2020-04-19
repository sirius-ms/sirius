package de.unijena.bioinf.ChemistryBase.ms.lcms;

import javax.annotation.Nonnull;

/**
 * A compound has traces of adducts and in-source fragments
 */
public class CompoundTrace extends IonTrace {

    @Nonnull protected final IonTrace[] adducts;
    @Nonnull protected final IonTrace[] inSourceFragments;

    public CompoundTrace(@Nonnull Trace[] isotopes, @Nonnull IonTrace[] adducts, @Nonnull IonTrace[] inSourceFragments) {
        super(isotopes);
        this.adducts = adducts;
        this.inSourceFragments = inSourceFragments;
    }


}
