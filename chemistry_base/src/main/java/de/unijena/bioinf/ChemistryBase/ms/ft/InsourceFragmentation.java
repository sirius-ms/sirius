package de.unijena.bioinf.ChemistryBase.ms.ft;

/**
 * A simple flag that can be used as LossAnnotation for In-Source fragmentations
 */
public class InsourceFragmentation {

    private final boolean isInsource;

    public InsourceFragmentation(boolean isInsource) {
        this.isInsource = isInsource;
    }

    public boolean isInsource() {
        return isInsource;
    }

}
