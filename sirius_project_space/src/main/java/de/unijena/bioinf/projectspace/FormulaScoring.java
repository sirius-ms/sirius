package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.scores.FormulaScore;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Set;

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
}
