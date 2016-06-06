package de.unijena.bioinf.ChemistryBase.fp;

public class SubstructureCountProperty extends SubstructureProperty {

    public SubstructureCountProperty(String smiles, int minimalCount) {
        super(smiles);
        this.minimalCount = minimalCount;
    }

    public SubstructureCountProperty(String smiles, String description, int minimalCount) {
        super(smiles, description);
        this.minimalCount = minimalCount;
    }

    public int minimalCount;

    public int getMinimalCount() {
        return minimalCount;
    }

    @Override
    public String toString() {
        return getSmiles() + " >= " + minimalCount;
    }

    @Override
    public String getDescription() {
        if (description!=null) {
            return toString() + " (" + description + ")";
        } else {
            return toString();
        }
    }
}
