package de.unijena.bioinf.ChemistryBase.fp;

public class ShortestPathProperty extends MolecularProperty {

    private final String descriptor;

    public ShortestPathProperty(String descriptor) {
        this.descriptor = descriptor;
    }

    /**
     * just for the case we want to change the string output. This will give the exact descriptor used in ShortestPathFingerprinter
     * @return
     */
    public String getShortestPathDescriptor() {
        return descriptor;
    }

    @Override
    public String getDescription() {
        return descriptor;
    }

    @Override
    public String toString() {
        return descriptor;
    }
}
