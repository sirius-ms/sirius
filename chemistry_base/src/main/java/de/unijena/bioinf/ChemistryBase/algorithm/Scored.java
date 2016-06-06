package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.ms.ft.Score;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scored<T> implements Comparable<Scored<T>> {

    private final T candidate;
    private final double score;

    public Scored(T candidate, double score) {
        this.candidate = candidate;
        this.score = score;
    }

    public T getCandidate() {
        return candidate;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("%s (%.3f)", candidate.toString(), score);
    }


    public static <T extends Object> Comparator<Scored<T>> desc() {
        return new Comparator<Scored<T>>() {
            @Override
            public int compare(Scored<T> o1, Scored<T> o2) {
                return Double.compare(o2.score,o1.score);
            }
        };
    }

    @Override
    public int compareTo(Scored<T> o) {
        return Double.compare(score, o.score);
    }
}
