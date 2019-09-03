package de.unijena.bioinf.ChemistryBase.algorithm.scoring;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;

public interface Score<T extends Score> extends DataAnnotation, Comparable<T> {
    double score();

    @Override
    default int compareTo(@NotNull T o) {
        return Double.compare(score(), o.score());
    }

    default String name() {
        return getClass().getSimpleName();
    }

    //todo this does not allow to define new scores within external packages?!
    static Class<? extends Score> resolve(String name) {
        {
            try {
                if (name.startsWith("de.unijena.bioinf"))
                    return (Class<? extends Score>)Class.forName(name);
                else return (Class<? extends Score>)Class.forName("de.unijena.bioinf." + name );
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException();
            }
        }
    }

    static String simplify(Class<? extends Score> klass) {
        return klass.getCanonicalName().replace("de.unijena.bioinf.", "");
    }

    abstract class AbstDoubleScore<T extends AbstDoubleScore> implements Score<T> {
        private final double score;

        protected AbstDoubleScore(double score) {
            this.score = score;
        }

        @Override
        public double score() {
            return score;
        }
    }

    class DoubleScore extends AbstDoubleScore<DoubleScore> {
        public DoubleScore(double score) {
            super(score);
        }
    }


}
