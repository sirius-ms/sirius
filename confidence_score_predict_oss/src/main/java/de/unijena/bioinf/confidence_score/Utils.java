package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.ChemDBs;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.util.Arrays;
import java.util.function.Predicate;

public class Utils {
    private Utils() {
    }

    public static Scored<FingerprintCandidate>[] condenseCandidatesByFlag(Scored<FingerprintCandidate>[] candidates, final long flags) {
        return Arrays.stream(candidates).filter(ChemDBs.inFilter(s -> s.getCandidate().getBitset(),flags)).toArray(Scored[]::new);
    }
}
