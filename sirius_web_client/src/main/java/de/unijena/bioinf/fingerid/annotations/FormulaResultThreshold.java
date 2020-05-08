package de.unijena.bioinf.fingerid.annotations;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Specifies if the list of Molecular Formula Identifications is filtered by a soft threshold
 * (calculateThreshold) before CSI:FingerID predictions are calculated.
 */

public class FormulaResultThreshold implements Ms2ExperimentAnnotation {
    //could be used as second property to specify different threshold types in the future
    private ThresholdCalculator thresholdCalculator = (topHitScore) -> Math.max(topHitScore, 0) - Math.max(5, topHitScore * 0.25);
    public final boolean value;


    public FormulaResultThreshold(boolean value) {
        this.value = value;
    }

    @DefaultInstanceProvider
    public static FormulaResultThreshold newInstance(@DefaultProperty boolean value) {
        return new FormulaResultThreshold(value);
    }

    public double calculateThreshold(double topHitScore) {
        return thresholdCalculator.calculateThreshold(topHitScore);
    }

    public void setThresholdCalculator(@NotNull ThresholdCalculator thresholdCalculator) {
        this.thresholdCalculator = thresholdCalculator;
    }

    public boolean useThreshold() {
        return value;
    }

    @FunctionalInterface
    public interface ThresholdCalculator {
        double calculateThreshold(double topHitScore);
    }
}
