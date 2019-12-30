package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Allows the USER to Specify the ScoreType that is used to rank the list of Molecular Formula Identifications
 * before CSI:FingerID predictions are calculated. Auto means that this ScoreType is
 * automatically set depending on the executed workflow.
 */
@DefaultProperty
public class FormulaResultRankingScore implements Ms2ExperimentAnnotation {
    public static final FormulaResultRankingScore AUTO = new FormulaResultRankingScore();

    public final Class<? extends FormulaScore> value;

    public boolean isAuto() {
        return value == null;
    }

    private FormulaResultRankingScore() {
        this((Class<? extends FormulaScore>) null);
    }
    public FormulaResultRankingScore(FormulaResultRankingScore container) {
        this(container.value);
    }
    public FormulaResultRankingScore(Class<? extends FormulaScore> value) {
        this.value = value;
    }

    //this is used for default property stuff
    public static FormulaResultRankingScore fromString(String value) {
        if (value == null || value.isEmpty() || value.toLowerCase().equals("null") || value.toLowerCase().equals("auto"))
            return AUTO;
        return new FormulaResultRankingScore((Class<? extends FormulaScore>) Score.resolve(value));
    }
}
