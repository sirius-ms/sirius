package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.util.Objects;

/**
 * Annotation to save the number of molecular formula candidates which are not further considered.
 * E.g. all which are not in the Top100.
 */
//todo this should be a Experiment Annotation!
public final class UnconsideredCandidatesUpperBound implements TreeAnnotation {
    private final int numberOfUnconsideredCandidates;
    private final double lowestConsideredCandidateScore;

    private static UnconsideredCandidatesUpperBound NO_RANKING_INVOLVED = new UnconsideredCandidatesUpperBound(-1, Double.NEGATIVE_INFINITY);
    public static UnconsideredCandidatesUpperBound noRankingInvolved() {
        return NO_RANKING_INVOLVED;
    }

    public UnconsideredCandidatesUpperBound(int numberOfUnconsideredCandidates, double lowestConsideredCandidateScore) {
        if (numberOfUnconsideredCandidates <0 && Double.isFinite(lowestConsideredCandidateScore)) throw new IllegalArgumentException("the number of unconsidered candidates must be positive.");
        this.numberOfUnconsideredCandidates = numberOfUnconsideredCandidates;
        this.lowestConsideredCandidateScore = lowestConsideredCandidateScore;
    }

    public int getNumberOfUnconsideredCandidates() {
        return numberOfUnconsideredCandidates;
    }

    public double getLowestConsideredCandidateScore() {
        return lowestConsideredCandidateScore;
    }

    public boolean isNoRankingInvolved() {
        return this.equals(NO_RANKING_INVOLVED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnconsideredCandidatesUpperBound that = (UnconsideredCandidatesUpperBound) o;
        return numberOfUnconsideredCandidates == that.numberOfUnconsideredCandidates &&
                Double.compare(that.lowestConsideredCandidateScore, lowestConsideredCandidateScore) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfUnconsideredCandidates, lowestConsideredCandidateScore);
    }
}
