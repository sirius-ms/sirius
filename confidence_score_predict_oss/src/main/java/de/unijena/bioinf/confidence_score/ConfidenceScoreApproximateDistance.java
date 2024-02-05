package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Allowed MCES distance between CSI:FingerID hit (best-scoring candidate) and true structure that should still count as correct identification.
 * Distance 0 corresponds to identical molecular structures. The closest non-identical structures have an MCES distance of 2 (cutting 2 bonds). It continues with 4,6,8 and so on.
 * Currently only 0 (exact) and 2 for approximate are supported.
 */
@DefaultProperty
public class ConfidenceScoreApproximateDistance implements Ms2ExperimentAnnotation {
    public final int value;

    public ConfidenceScoreApproximateDistance() {
        this(2);
    }

    public ConfidenceScoreApproximateDistance(int value) {
        //currently we only allow a value of 2
        if (value==0) throw new IllegalArgumentException("Only MCES distances greater 0 are allowed. Distance 0 is the exact mode.");
        if (value>2) throw new IllegalArgumentException("Currently, only the 0 (exact) and 2 (closest non-identical match) are supported as ConfidenceApproximateDistance (MCES).");
        if (value%2 != 0) throw new IllegalArgumentException("Only distances that are powers of two  are supported as ConfidenceApproximateDistance (MCES).");
        this.value = value;
    }
}
