package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * if this annotation is set, recalibration is ommited
 */
@DefaultProperty
public enum ForbidRecalibration implements Ms2ExperimentAnnotation {
    ALLOWED(false),
    FORBIDDEN(true);

    private final boolean recalibrationForbidden;

    ForbidRecalibration(boolean recalibrationForbidden) {
        this.recalibrationForbidden = recalibrationForbidden;
    }

    public boolean isForbidden() {
        return recalibrationForbidden;
    }

    public boolean isAllowed() {
        return !recalibrationForbidden;
    }
}
