package de.unijena.bioinf.ChemistryBase.ms.ft;

/**
 * Annotation to save the number of molecular formula candidates which are not further considered.
 * E.g. all which are not in the Top100.
 */
public class UnconsideredCandidatesUpperBound {
    private int numberOfUnconsideredCandidates;
    private double lowestConsideredCandidateScore;

    public UnconsideredCandidatesUpperBound(int numberOfUnconsideredCandidates, double lowestConsideredCandidateScore) {
        if (numberOfUnconsideredCandidates <0) throw new IllegalArgumentException("the number of unconsidered candidates must be positive.");
        this.numberOfUnconsideredCandidates = numberOfUnconsideredCandidates;
        this.lowestConsideredCandidateScore = lowestConsideredCandidateScore;
    }

    public int getNumberOfUnconsideredCandidates() {
        return numberOfUnconsideredCandidates;
    }

    public double getLowestConsideredCandidateScore() {
        return lowestConsideredCandidateScore;
    }
}
