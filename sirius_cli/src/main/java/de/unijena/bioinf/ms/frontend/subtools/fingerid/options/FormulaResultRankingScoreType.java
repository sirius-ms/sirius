package de.unijena.bioinf.ms.frontend.subtools.fingerid.options;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.projectspace.sirius.FormulaResultRankingScore;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

/**
 * Specifies the Score that is used to rank the list Molecular Formula Identifications
 * before CSI:FingerID predictions are calculated.
 */

public enum FormulaResultRankingScoreType {
    // just to define a selection of scores that are selectable by the user
    AUTO(null),
    TREE(TreeScore.class),
    ISOTOPE(IsotopeScore.class),
    SIRIUS(SiriusScore.class),
    ZODIAC(ZodiacScore.class);

    private final Class<? extends FormulaScore> clazz;

    FormulaResultRankingScoreType(Class<? extends FormulaScore> clazz) {
        this.clazz = clazz;
    }

    public String clazzName() {
        return clazz().getName();
    }

    public String simpleClazzName() {
        return Score.simplify(clazz);
    }

    public Class<? extends FormulaScore> clazz() {
        return clazz;
    }

    public boolean isDefined() {
        return !this.equals(AUTO);
    }
}
