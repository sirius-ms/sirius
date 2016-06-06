package de.unijena.bioinf.ChemistryBase.fp;

public class SubstructureProperty extends MolecularProperty {

    protected final String smiles, description;

    public SubstructureProperty(String smiles) {
        this(smiles,null);
    }

    public SubstructureProperty(String smiles, String description) {
        this.smiles = smiles;
        this.description = description;
    }

    public String getSmiles() {
        return smiles;
    }

    @Override
    public String getDescription() {
        if (description!=null) {
            return smiles + " (" + description + ")";
        } else {
            return smiles;
        }
    }

    @Override
    public String toString() {
        return smiles;
    }



}
