package de.unijena.bioinf.ChemistryBase.fp;

public class SubstructureProperty extends MolecularProperty {

    protected final String smarts, description;

    public SubstructureProperty(String smarts) {
        this(smarts,null);
    }

    public SubstructureProperty(String smarts, String description) {
        this.smarts = smarts;
        this.description = description;
    }

    public String getSmarts() {
        return smarts;
    }

    @Override
    public String getDescription() {
        if (description!=null) {
            return smarts + " (" + description + ")";
        } else {
            return smarts;
        }
    }

    @Override
    public String toString() {
        return smarts;
    }



}
