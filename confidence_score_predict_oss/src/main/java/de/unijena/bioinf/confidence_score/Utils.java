package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.util.Arrays;

public class Utils {
    private Utils() {
    }

    public static Scored<FingerprintCandidate>[] condenseCandidatesByFlag(Scored<FingerprintCandidate>[] candidates, long flags) {
        if (flags == 0)
            return candidates;

        return Arrays.stream(candidates).filter(it -> (it.getCandidate().getBitset() & flags) != 0).toArray(Scored[]::new);
    }
}
