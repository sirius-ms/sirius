package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.util.Arrays;
import java.util.function.Predicate;

public class Utils {
    private Utils() {
    }

    public static Predicate<FingerprintCandidate> getCandidateByFlagFilter(final long dbFlag) {
        return it -> (dbFlag == 0 || (it.getBitset() & dbFlag) != 0);
    }

    public static Scored<FingerprintCandidate>[] condenseCandidatesByFlag(Scored<FingerprintCandidate>[] candidates, long flags) {
        if (flags == 0)
            return candidates;

        return Arrays.stream(candidates).map(Scored::getCandidate).filter(getCandidateByFlagFilter(flags)::test).toArray(Scored[]::new);
    }
}
