package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.Reaction;
import de.unijena.bioinf.GibbsSampling.model.ReactionStepSizeScorer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ReactionScorer implements EdgeScorer {
    private ReactionStepSizeScorer scorer;
    private Reaction[] reactions;
    private Map<MolecularFormula, List<Reaction>> reactionNetChangeMap;

    public ReactionScorer(Reaction[] reactions, ReactionStepSizeScorer scorer) {
        this.reactions = reactions;
        this.scorer = scorer;
        this.reactionNetChangeMap = new HashMap();
        Reaction[] var3 = reactions;
        int var4 = reactions.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Reaction reaction = var3[var5];
            MolecularFormula netChange = reaction.netChange();
            if(!this.reactionNetChangeMap.containsKey(netChange)) {
                this.reactionNetChangeMap.put(netChange, new ArrayList());
            }

            this.reactionNetChangeMap.get(netChange).add(reaction);
        }

    }

    public void prepare(Candidate[][] candidates) {
    }

    public double score(Candidate candidate1, Candidate candidate2) {
        throw new NoSuchMethodError("has to be debugged");
//        MolecularFormula formula1 = candidate1.getAnnotation(MolecularFormula.class);
//        MolecularFormula formula2 = candidate2.getAnnotation(MolecularFormula.class);
//        List reactions = (List)this.reactionNetChangeMap.get(formula1.subtract(formula2));
//        double maxScore = 0.0D;
//        Iterator var8;
//        Reaction reaction;
//        if(reactions != null) {
//            var8 = reactions.iterator();
//
//            while(var8.hasNext()) {
//                reaction = (Reaction)var8.next();
//                if(reaction.hasReactionAnyDirection(formula1, formula2)) {
//                    maxScore = Math.max(this.scorer.multiplier(reaction.stepSize()), maxScore);
//                }
//            }
//        }
//
//        reactions = (List)this.reactionNetChangeMap.get(formula2.subtract(formula1));
//        if(reactions != null) {
//            var8 = reactions.iterator();
//
//            while(var8.hasNext()) {
//                reaction = (Reaction)var8.next();
//                if(reaction.hasReactionAnyDirection(formula2, formula1)) {
//                    maxScore = Math.max(this.scorer.multiplier(reaction.stepSize()), maxScore);
//                }
//            }
//        }
//
//        return maxScore;
    }

    @Override
    public double scoreWithoutThreshold(Candidate var1, Candidate var2) {
        throw new NoSuchMethodError("has to be debugged");
    }

    @Override
    public void setThreshold(double threshold) {
        throw new NoSuchMethodError("has to be debugged");
    }

    @Override
    public double getThreshold() {
        throw new NoSuchMethodError("has to be debugged");
    }

    public void clean() {
    }

    public double[] normalization(Candidate[][] candidates, double minimum_number_matched_peaks_losses) {
        return null;
    }
}
