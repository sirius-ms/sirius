package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * contains any score associated with a molecular formula. This includes:
 * - tree score
 * - isotope scores
 * - zodiac scores
 * - top csi score
 * - confidence score
 *
 * Scores are stored in a HashMap with a class as key. Each score can be logarithmic or probabilistic
 *
 * All subclasses of FormulaScore have the following restrictions:
 * - constructor with single double parameter
 * - final
 */
public class FormulaScoring implements Iterable<FormulaScore>, Annotated<FormulaScore>, DataAnnotation {
    private final Annotations<FormulaScore> scores;

    public FormulaScoring(Set<FormulaScore> scores) {
        this();
        scores.forEach(score -> setAnnotation((Class<FormulaScore>) score.getClass(), score));
    }

    public FormulaScoring() {
        this.scores = new Annotations<>();
    }


    public <T extends FormulaScore> void addAnnotation(Class<T> klass, double value) {
        try {
            addAnnotation(klass, klass.getConstructor(double.class).newInstance(value));
        } catch (NoSuchMethodException|IllegalAccessException| InvocationTargetException|InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Annotations<FormulaScore> annotations() {
        return scores;
    }

    @NotNull
    @Override
    public Iterator<FormulaScore> iterator() {
        return annotations().valueIterator();
    }

    public static Comparator<FormulaScoring> comparingMultiScore(List<Class<? extends FormulaScore>> scoreTypes) {
        return comparingMultiScore(scoreTypes,true);
    }

    public static Comparator<FormulaScoring> comparingMultiScore(List<Class<? extends FormulaScore>> scoreTypes, boolean descending) {
        if (scoreTypes == null || scoreTypes.isEmpty())
            throw new IllegalArgumentException("NO score type given");

        Comparator<FormulaScoring> comp = Comparator.comparing(s -> s == null ? FormulaScore.NA(scoreTypes.get(0)): s.getAnnotationOr(scoreTypes.get(0), FormulaScore::NA));
        for (Class<? extends FormulaScore> type : scoreTypes.subList(1, scoreTypes.size()))
            comp = comp.thenComparing(s -> s == null ? FormulaScore.NA(type): s.getAnnotationOr(type, FormulaScore::NA));

        return descending ? comp.reversed() : comp;
    }

    public static <T extends FormulaResult> List<SScored<T, ? extends FormulaScore>> reRankBy(@Nullable List<? extends SScored<T, ? extends FormulaScore>> data, @NotNull Class<? extends FormulaScore> scoreType, boolean descending) {
        return reRankBy(data, Collections.singletonList(scoreType), descending);
    }

    public static <T extends FormulaResult> List<SScored<T, ? extends FormulaScore>> reRankBy(@Nullable List<? extends SScored<T, ? extends FormulaScore>> data, @NotNull List<Class<? extends FormulaScore>> scoreTypes, boolean descending) {
        if (scoreTypes.isEmpty())
            throw new IllegalArgumentException("NO score type given");

        if (data == null)
            return null;

        if (data.isEmpty())
            return Collections.emptyList();

        return data.stream().map(SScored::getCandidate)
                .sorted((c1,c2) -> comparingMultiScore(scoreTypes,descending).compare(c1.getAnnotationOrNull(FormulaScoring.class), c2.getAnnotationOrNull(FormulaScoring.class)))
                .map(c -> c.hasAnnotation(FormulaScoring.class)
                        ? new SScored<>(c, c.getAnnotationOrThrow(FormulaScoring.class).getAnnotationOr(scoreTypes.get(0), FormulaScore::NA))
                        : new SScored<>(c, FormulaScore.NA(scoreTypes.get(0)))).collect(Collectors.toList());
    }
}
