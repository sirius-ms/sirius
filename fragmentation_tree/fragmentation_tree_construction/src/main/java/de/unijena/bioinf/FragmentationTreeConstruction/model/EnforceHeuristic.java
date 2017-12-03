package de.unijena.bioinf.FragmentationTreeConstruction.model;

/**
 * If this annotation is set, only heuristic scores are reported and
 * the exact computation is ommitted.
 *
 * TODO: Not implemented yet!
 */
public class EnforceHeuristic {

    public static final EnforceHeuristic EXACT = new EnforceHeuristic(false), HEURISTIC = new EnforceHeuristic(true);

    private final boolean heuristic;

    public EnforceHeuristic(boolean heuristic) {
        this.heuristic = heuristic;
    }

    public boolean isOnlyComputingHeuristic() {
        return heuristic;
    }

    public boolean isExact() {
        return !heuristic;
    }
}
