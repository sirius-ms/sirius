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
        /*if (flags == 0)
            return candidates;


        Scored<FingerprintCandidate>[] condensed;
        ArrayList<Scored<FingerprintCandidate>> condensed_as_list= new ArrayList<>();


        for(int i=0;i<candidates.length;i++){
            if ((candidates[i].getCandidate().getBitset() & flags)!=0){
                condensed_as_list.add(candidates[i]);
            }

        }

        condensed = new Scored[condensed_as_list.size()];

        condensed=  condensed_as_list.toArray(condensed);*/


//        return condensed;

        //TODO @Markus cleanup reverted weil error
        return Arrays.stream(candidates).filter(s -> getCandidateByFlagFilter(flags).test(s.getCandidate())).toArray(Scored[]::new);
    }
}
