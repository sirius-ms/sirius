package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;

public enum ConfidenceMode implements DescriptiveOptions {
    //todo would be great to standardize all the different confidence score mode options and enums in the whole sirius project with this as a base class.
    EXACT("Use confidence score in exact mode: Only molecular structures identical to the true structure should count as correct identification."),
    APPROXIMATE("Use confidence score in approximate mode: Molecular structures hits that are close to the true structure should count as correct identification.");

    private final String description;

    ConfidenceMode(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
