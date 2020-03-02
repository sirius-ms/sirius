package de.unijena.bioinf.ChemistryBase.algorithm.scoring;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public interface Score<T extends Score> extends DataAnnotation, Comparable<T> {

    double score();

    @Override
    default int compareTo(@NotNull T o) {
        final double a = score();
        if (Double.isNaN(a)) return -1; // NaN should be treated as "minimum score"
        final double b = o.score();
        if (Double.isNaN(b)) return 1;
        return Double.compare(a, b);
    }

    default String name() {
        return getClass().getSimpleName();
    }


    //todo this does not allow to define new scores within external packages?!
    static Class<? extends Score> resolve(String name) {
        if (name == null || name.isEmpty() || name.toLowerCase().equals("null"))
            return null;

        try {
            if (name.startsWith("de.unijena.bioinf"))
                return (Class<? extends Score>) Class.forName(name);
            else return (Class<? extends Score>) Class.forName("de.unijena.bioinf." + name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException();
        }
    }

    static String simplify(Class<? extends Score<?>> klass) {
        return klass.getCanonicalName().replace("de.unijena.bioinf.", "");
    }


    abstract class AbstDoubleScore<T extends AbstDoubleScore<?>> implements Score<T> {
        private static final Map<Class<? extends Score<?>>, Score<?>> MISSINGS = new HashMap<>();

        private final double score;

        protected AbstDoubleScore(double score) {
            this.score = score;
        }

        @Override
        public double score() {
            return score;
        }

        public String toString() {
            return Double.isNaN(score) ? NA() : String.valueOf(score());
        }

        public static String NA() {
            return "N/A";
        }

        public synchronized static <T extends Score<?>> T NA(@NotNull Class<T> scoreType, double missingValue) {
            T inst = (T) MISSINGS.get(scoreType);
            if (inst == null) {
                try {
                    inst = scoreType.getConstructor(double.class).newInstance(missingValue);
                    MISSINGS.put(scoreType, inst);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
            return inst;
        }
    }

    class DoubleScore extends AbstDoubleScore<DoubleScore> {
        public DoubleScore(double score) {
            super(score);
        }
    }


}
