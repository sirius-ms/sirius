package de.unijena.bioinf.ChemistryBase.ms.ft;

/**
 * Annotation to save the number of molecular formula candidates which are not further considered.
 * E.g. all which are not in the Top100.
 */
public class UnregardedCandidatesUpperBound {
    private int numberOfUnregardedCandidates;
    private double lowestConsideredCandidateScore;

    public UnregardedCandidatesUpperBound(int numberOfUnregardedCandidates, double lowestConsideredCandidateScore) {
        this.numberOfUnregardedCandidates = numberOfUnregardedCandidates;
        this.lowestConsideredCandidateScore = lowestConsideredCandidateScore;
    }

    public int getNumberOfUnregardedCandidates() {
        return numberOfUnregardedCandidates;
    }

    public double getLowestConsideredCandidateScore() {
        return lowestConsideredCandidateScore;
    }
}
