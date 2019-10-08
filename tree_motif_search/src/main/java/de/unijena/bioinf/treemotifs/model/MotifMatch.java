package de.unijena.bioinf.treemotifs.model;

public class MotifMatch {

    protected final double TotalProbability, maxProbability;
    protected final long[] matchingFragments, matchingRootLosses;

    MotifMatch(double totalProbability, double maxProbability, long[] matchingFragments, long[] matchingRootLosses) {
        TotalProbability = totalProbability;
        this.maxProbability = maxProbability;
        this.matchingFragments = matchingFragments;
        this.matchingRootLosses = matchingRootLosses;
    }

    public double getTotalProbability() {
        return TotalProbability;
    }

    public double getMaxProbability() {
        return maxProbability;
    }

    public long[] getMatchingFragments() {
        return matchingFragments;
    }

    public long[] getMatchingRootLosses() {
        return matchingRootLosses;
    }
}
