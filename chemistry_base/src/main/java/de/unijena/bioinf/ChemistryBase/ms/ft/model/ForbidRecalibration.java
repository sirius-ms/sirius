package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

/**
 * if this annotation is set, recalibration is ommited
 */
public class ForbidRecalibration implements Ms2ExperimentAnnotation {

    public final static ForbidRecalibration ALLOWED = new ForbidRecalibration(false);
    public final static ForbidRecalibration FORBIDDEN = new ForbidRecalibration(true);

    private final boolean recalibrationForbidden;

    private ForbidRecalibration(boolean recalibrationForbidden) {
        this.recalibrationForbidden = recalibrationForbidden;
    }

    public boolean isForbidden() {
        return recalibrationForbidden;
    }

    public boolean isAllowed() {
        return !recalibrationForbidden;
    }
}
