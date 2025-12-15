package de.unijena.bioinf.ms.persistence.model.core;

import lombok.Getter;


@Getter
public enum DefaultQualityCategory {
    PEAK_QUALITY("Peak Quality"),
    ALIGNMENT_QUALITY("Alignment Quality"),
    ISOTOPE_QUALITY("Isotope Pattern Quality"),
    MS2_QUALITY("Fragmentation Pattern Quality"),
    ADDUCT_QUALITY("Adduct Assignment Quality");

    private final String displayName;

    DefaultQualityCategory(String displayName) {
        this.displayName = displayName;
    }
}
