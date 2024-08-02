package de.unijena.bioinf.ChemistryBase.utils;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, nullable = true)
public enum DataQuality {

    /**
     * If a subcategory is "NOT_APPLICABLE" then it should not be considered for quality check
     * (e.g. a feature without MS/MS has not a LOWEST MS/MS quality just because it has no MS/MS)
     */
    NOT_APPLICABLE(0f),

    /**
     * Features with the lowest quality are probably not real signals. We would rather delete then.
     */
    LOWEST(0f),

    /**
     * features with bad quality are maybe real features. Still, we don't want to list them
     * in our feature list by default.
     */
    BAD(1f),

    /**
     * Real signals with decent quality.
     */
    DECENT(2f),

    /**
     * Real signal with good quality.
     */
    GOOD(3f);

    private final float score;

    DataQuality(float score) {
        this.score = score;
    }

    public float getScore() {
        return score;
    }
}
