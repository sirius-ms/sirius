package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * Specifies the Score that is used to rank the list Molecular Formula Identifications
 * before CSI:FingerID predictions are calculated.
 */
@DefaultProperty
public class FormulaResultRankingScore implements Ms2ExperimentAnnotation {
    public final Class<? extends FormulaScore> value;

    private FormulaResultRankingScore() {
        this((Class<? extends FormulaScore>) null);
    }

    public FormulaResultRankingScore(FormulaResultRankingScore container) {
        this(container.value);
    }
    public FormulaResultRankingScore(Class<? extends FormulaScore> value) {
        this.value = value;
    }
}
