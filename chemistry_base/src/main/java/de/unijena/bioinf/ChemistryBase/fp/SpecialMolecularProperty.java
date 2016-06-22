package de.unijena.bioinf.ChemistryBase.fp;

public class SpecialMolecularProperty extends MolecularProperty {

    private final String description;
    private SubstructureCountProperty alternativeDescription;

    public SpecialMolecularProperty(String description, SubstructureCountProperty alternativeDescription) {
        this.description = description;
        this.alternativeDescription = alternativeDescription;
    }

    public SpecialMolecularProperty(String description) {
        this.description = description;
        this.alternativeDescription = null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public SubstructureCountProperty describeAsSubstructureCount() {
        return alternativeDescription;
    }


}
