package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.ms.Peak;

/**
 * An annotation for pseudo losses and pseudo fragments.
 * These objects have to be removed from the tree after computation. But their information might be
 * transferred into some annotation object of the tree.
 */
public class IsotopicMarker {

    private Peak peak;

    public IsotopicMarker() {

    }

    public Peak getPeak() {
        return peak;
    }
}
