package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

/**
 * A simple flag that can be used as LossAnnotation for In-Source fragmentations
 */
public final class InsourceFragmentation implements TreeAnnotation {

    private final boolean isInsource;

    private final static InsourceFragmentation IS = new InsourceFragmentation(true), ISNOT = new InsourceFragmentation(false);

    public static InsourceFragmentation yes() {
        return IS;
    }

    public static InsourceFragmentation no() {
        return ISNOT;
    }

    public InsourceFragmentation(boolean isInsource) {
        this.isInsource = isInsource;
    }

    public boolean isInsource() {
        return isInsource;
    }

}
