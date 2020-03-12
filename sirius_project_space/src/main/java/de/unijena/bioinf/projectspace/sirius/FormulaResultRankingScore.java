package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Allows the USER to Specify the ScoreType that is used to rank the list of Molecular Formula Identifications
 * before CSI:FingerID predictions are calculated. Auto means that this ScoreType is
 * automatically set depending on the executed workflow.
 */
@DefaultProperty
public class FormulaResultRankingScore implements Ms2ExperimentAnnotation {
    public static final FormulaResultRankingScore AUTO = new FormulaResultRankingScore();

    public final List<Class<? extends FormulaScore>> value;

    public boolean isAuto() {
        return value == null;
    }

    private FormulaResultRankingScore() {
        this((List<Class<? extends FormulaScore>>) null);
    }

    public FormulaResultRankingScore(FormulaResultRankingScore container) {
        this(container.value);
    }

    public FormulaResultRankingScore(List<Class<? extends FormulaScore>> value) {
        this.value = (value != null && value.isEmpty()) ? null : value;
    }

    public List<Class<? extends FormulaScore>> value() {
        return value;
    }

    //this is used for default property stuff
    public static Optional<FormulaResultRankingScore> parseFromString(String value) {
        try {
            return Optional.of(fromString(value));
        } catch (Exception e) {
            LoggerFactory.getLogger("Could not Parse FormulaResultRankingScores '" + value + "'!");
            return Optional.empty();
        }
    }

    public static FormulaResultRankingScore fromString(String value) {
        if (value == null || value.isBlank() || value.toLowerCase().equals("null") || value.toLowerCase().equals("auto"))
            return AUTO;
        return new FormulaResultRankingScore(Arrays.stream(value.split(",")).map(String::strip).map(v -> (Class<? extends FormulaScore>) Score.resolve(v)).collect(Collectors.toList()));
    }
}
