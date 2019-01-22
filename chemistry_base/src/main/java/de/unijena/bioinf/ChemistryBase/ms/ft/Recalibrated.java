package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public final class Recalibrated implements TreeAnnotation {

    protected final double recalibrationBonus, recalibrationPenalty;

    public static final String PENALTY_KEY = "RecalibrationPenalty";

    protected final static Recalibrated NOT_RECALIBRATED = new Recalibrated(0d,0d);

    public Recalibrated(double recalibrationBonus, double recalibrationPenalty) {
        this.recalibrationBonus = recalibrationBonus;
        this.recalibrationPenalty = recalibrationPenalty;
    }

    public static Recalibrated noRecalibration() {
        return NOT_RECALIBRATED;
    }

    public static Recalibrated isRecalibrated(double recalibrationBonus, double recalibrationPenalty) {
        return new Recalibrated(recalibrationBonus, recalibrationPenalty);
    }

    public boolean isRecalibrated() {
        return recalibrationBonus>0;
    }

    public double score() {
        return recalibrationBonus-recalibrationPenalty;
    }

}
