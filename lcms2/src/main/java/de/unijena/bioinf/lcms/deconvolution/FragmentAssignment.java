package de.unijena.bioinf.lcms.deconvolution;

import java.util.Locale;

/**
 * What one fragment belongs to: a sparse probability vector over the precursor features of its channel, plus the
 * probability that it belongs to none of them.
 * <p>
 * Assignment is soft, because a fragment that co-elutes with two precursors in the same isolation window genuinely
 * cannot be attributed to one of them from a single sample. It is also sparse: a precursor that holds a per cent
 * of the probability is not worth carrying around, so entries below a threshold are dropped.
 * <p>
 * The probabilities that remain are the ones the walk computed - they are neither renormalised over the survivors
 * nor is the dropped mass moved to the unassigned share. Renormalising would inflate the leading precursor to a
 * certainty it never had, and calling the dropped mass unassigned would misreport a negligible alternative as
 * evidence of nothing. So a stored vector sums to slightly less than one, and the missing part is exactly the
 * mass that was too small to keep. Modelled on {@code adducts.assignment.AdductAssignment}.
 */
public class FragmentAssignment {

    /** below this a precursor is not worth storing for this fragment */
    public static final double DEFAULT_MIN_PROBABILITY = 0.01;

    private static final FragmentAssignment NOTHING = new FragmentAssignment(new int[0], new double[0], 1d);

    /** precursor indices, ordered by descending probability */
    private final int[] precursorIndices;

    private final double[] probabilities;

    private final double unassignedProbability;

    private FragmentAssignment(int[] precursorIndices, double[] probabilities, double unassignedProbability) {
        this.precursorIndices = precursorIndices;
        this.probabilities = probabilities;
        this.unassignedProbability = unassignedProbability;
    }

    /**
     * @return an assignment that claims nothing, for a fragment with no candidate at all
     */
    public static FragmentAssignment unassigned() {
        return NOTHING;
    }

    /**
     * @param precursorIndices     candidate precursors, in any order
     * @param probabilities        their probabilities, aligned with {@code precursorIndices}
     * @param unassignedProbability probability that the fragment belongs to none of them
     * @param minProbability       candidates below this are dropped
     */
    public static FragmentAssignment of(int[] precursorIndices, double[] probabilities,
                                        double unassignedProbability, double minProbability) {
        if (precursorIndices.length != probabilities.length)
            throw new IllegalArgumentException("one probability per precursor, got " + precursorIndices.length
                    + " and " + probabilities.length);
        int kept = 0;
        for (double p : probabilities) if (p >= minProbability) ++kept;
        if (kept == 0) return new FragmentAssignment(new int[0], new double[0], unassignedProbability);

        final int[] indices = new int[kept];
        final double[] probs = new double[kept];
        int at = 0;
        for (int k = 0; k < probabilities.length; ++k) {
            if (probabilities[k] >= minProbability) {
                indices[at] = precursorIndices[k];
                probs[at] = probabilities[k];
                ++at;
            }
        }
        // descending by probability; insertion sort, since there are only ever a handful of candidates
        for (int i = 1; i < kept; ++i) {
            final int index = indices[i];
            final double prob = probs[i];
            int j = i - 1;
            while (j >= 0 && probs[j] < prob) {
                indices[j + 1] = indices[j];
                probs[j + 1] = probs[j];
                --j;
            }
            indices[j + 1] = index;
            probs[j + 1] = prob;
        }
        return new FragmentAssignment(indices, probs, unassignedProbability);
    }

    /**
     * @return number of precursors this fragment is stored for
     */
    public int size() {
        return precursorIndices.length;
    }

    public boolean isEmpty() {
        return precursorIndices.length == 0;
    }

    /**
     * @param rank 0 is the most likely precursor
     */
    public int precursorIndexAt(int rank) {
        return precursorIndices[rank];
    }

    public double probabilityAt(int rank) {
        return probabilities[rank];
    }

    public double getUnassignedProbability() {
        return unassignedProbability;
    }

    /**
     * @return probability of the most likely precursor, or zero if there is none
     */
    public double probabilityOfMostLikely() {
        return probabilities.length == 0 ? 0d : probabilities[0];
    }

    /**
     * @return true if this fragment is more likely to belong to none of the candidates than to any single one of
     * them
     */
    public boolean likelyBelongsToNothing() {
        return unassignedProbability > probabilityOfMostLikely();
    }

    /**
     * @return true if more than one precursor is stored, i.e. a single sample could not attribute this fragment
     */
    public boolean isAmbiguous() {
        return precursorIndices.length > 1;
    }

    /**
     * @return summed probability of the stored precursors. Together with the unassigned share this falls short of
     * one by the mass that was dropped as negligible.
     */
    public double storedProbability() {
        double sum = 0d;
        for (double p : probabilities) sum += p;
        return sum;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        for (int k = 0; k < precursorIndices.length; ++k) {
            if (k > 0) buf.append(", ");
            buf.append(String.format(Locale.US, "precursor %d: %.0f %%", precursorIndices[k], probabilities[k] * 100));
        }
        if (buf.isEmpty() || unassignedProbability > 0.005) {
            if (!buf.isEmpty()) buf.append(", ");
            buf.append(String.format(Locale.US, "none: %.0f %%", unassignedProbability * 100));
        }
        return buf.toString();
    }
}
