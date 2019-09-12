package de.unijena.bioinf.ms.frontend.subtools.fingerid.annotations;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

/**
 * Specifies the Score that is used to rank the list Molecular Formula Identifications
 * before CSI:FingerID predictions are calculated.
 */
@DefaultProperty
public class UserFormulaResultRankingScore implements Ms2ExperimentAnnotation {

    public final Score value;

    private UserFormulaResultRankingScore() {
        this(null);
    }

    public UserFormulaResultRankingScore(Score value) {
        this.value = value;
    }

    public boolean isDefined() {
        return !isAuto();
    }

    public boolean isAuto() {
        return value == Score.AUTO;
    }

    public Class<? extends FormulaScore> getScoreClass() {
        return value.clazz();
    }


    // just to define a selection of scores that are selectable by the user
    public enum Score {
        AUTO(null),
        TREE(TreeScore.class),
        ISOTOPE(IsotopeScore.class),
        SIRIUS(SiriusScore.class),
        ZODIAC(ZodiacScore.class);

        private final Class<? extends FormulaScore> clazz;

        Score(Class<? extends FormulaScore> clazz) {
            this.clazz = clazz;
        }

        public String clazzName() {
            return clazz().getName();
        }

        public Class<? extends FormulaScore> clazz() {
            return clazz;
        }
    }


}
