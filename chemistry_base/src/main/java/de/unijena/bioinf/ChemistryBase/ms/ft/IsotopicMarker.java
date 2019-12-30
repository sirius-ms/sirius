package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

/**
 * An annotation for pseudo losses and pseudo fragments.
 * These objects have to be removed from the tree after computation. But their information might be
 * transferred into some annotation object of the tree.
 */
public class IsotopicMarker implements DataAnnotation {

    private static final IsotopicMarker IS_ISOTOPE = new IsotopicMarker(true), IS_NOT = new IsotopicMarker(false);

    private final boolean isotope;

    protected IsotopicMarker() {
        this(false);
    }

    private IsotopicMarker(boolean is) {
        this.isotope = is;
    }

    public boolean isIsotope() {
        return isotope;
    }

    public static IsotopicMarker is() {
        return IS_ISOTOPE;
    }

    public static IsotopicMarker isNot() {
        return IS_NOT;
    }



}
