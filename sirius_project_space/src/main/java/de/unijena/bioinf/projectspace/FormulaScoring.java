package de.unijena.bioinf.projectspace;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;

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
public class FormulaScoring implements Iterable<FormulaScore> {

    private final HashMap<Class<? extends FormulaScore>, FormulaScore> scores;

    public FormulaScoring() {
        this.scores = new HashMap<>();
    }

    public <T extends FormulaScore> void set(Class<T> klass, T value) {
        scores.put(klass, value);
    }
    public <T extends FormulaScore> void set(Class<T> klass, double value) {
        try {
            set(klass, klass.getConstructor(double.class).newInstance(value));
        } catch (NoSuchMethodException|IllegalAccessException| InvocationTargetException|InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends FormulaScore> T get(Class<T> klass) {
        return (T)scores.get(klass);
    }

    public Class<? extends FormulaScore> resolve(String name) {
        {
            try {
                if (name.startsWith("de"))
                    return (Class<? extends FormulaScore>)Class.forName(name);
                else return (Class<? extends FormulaScore>)Class.forName("de.unijena.bioinf.projectspace." + name );
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException();
            }
        }
    }
    public String simplify(Class<? extends FormulaScore> klass) {
        return klass.getCanonicalName().replace("de.unijena.bioinf.projectspace.", "");
    }

    @NotNull
    @Override
    public Iterator<FormulaScore> iterator() {
        return scores.values().iterator();
    }
}
