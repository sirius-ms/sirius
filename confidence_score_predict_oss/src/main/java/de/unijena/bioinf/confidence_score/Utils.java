package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by martin on 25.06.18.
 */
public class Utils {



    public Scored<FingerprintCandidate>[] condense_candidates_by_flag(Scored<FingerprintCandidate>[] candidates, long flags){

        Scored<FingerprintCandidate>[] condensed;
        ArrayList<Scored<FingerprintCandidate>> condensed_as_list= new ArrayList<>();

        if(flags==0){
            return candidates;
        }

        for(int i=0;i<candidates.length;i++){
            if ((candidates[i].getCandidate().getBitset() & flags)!=0){
                condensed_as_list.add(candidates[i]);
            }

        }


        condensed = new Scored[condensed_as_list.size()];

        condensed=  condensed_as_list.toArray(condensed);


        return condensed;
    }
}
