package de.unijena.bioinf.ChemistryBase.ms.fp;

/**
 * Created by kaidu on 31.05.16.
 */
public class SpecialMolecularProperty extends MolecularProperty {

    private final String description;

    public SpecialMolecularProperty(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
