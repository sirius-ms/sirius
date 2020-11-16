package de.unijena.bioinf.ChemistryBase.ms.lcms;

public enum QuantificationMeasure {

    APEX("Abundance is the highest intensity of a chromatographic peak"),
    INTEGRAL("Abundance is the integral over the chromatographic peak"),
    INTEGRAL_FWHMD("Abundance is the integral over the full-width-halve-maximum area of the chromatographic peak");

    public final String description;

    QuantificationMeasure(String description) {
        this.description = description;
    }
}
