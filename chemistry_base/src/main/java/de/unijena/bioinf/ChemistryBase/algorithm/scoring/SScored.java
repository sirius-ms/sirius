package de.unijena.bioinf.ChemistryBase.algorithm.scoring;

public class SScored<T, S extends Score> implements Comparable<SScored<T, S>> {

    private final T candidate;
    private final S score;

    public SScored(T candidate, S score) {
        this.candidate = candidate;
        this.score = score;
    }

    public T getCandidate() {
        return candidate;
    }

    public S getScoreObject() {
        return score;
    }

    public double getScore() {
        return score.score();
    }

    @Override
    public String toString() {
        return String.format("%s (%.3f)", candidate.toString(), score.score());
    }

    @Override
    public int compareTo(SScored<T, S> o) {
        return score.compareTo(o.score);
    }
}
