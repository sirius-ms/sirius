package de.unijena.bioinf.ChemistryBase.utils;

import lombok.Getter;

/**
 * The dimensions in which the quality of an LC-MS feature is assessed - each graded on the {@link DataQuality}
 * scale by one of the quality checks of the LC-MS processing. "Default" because a quality report is keyed by
 * category id and may carry further categories; these are the ones SIRIUS always assesses. The {@link #name()}
 * is the id a report, the search index ({@code qualities.<id>}) and the REST API use for the category.
 */
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
