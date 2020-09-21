/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.GibbsSampling.model.Candidate;
import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.Reaction;
import de.unijena.bioinf.GibbsSampling.model.ReactionStepSizeScorer;
import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.ArrayList;
import java.util.HashMap;
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

    @Override
    public BasicJJob<Object> getPrepareJob(Candidate[][] var1) {
        return new BasicJJob<Object>() {
            @Override
            protected Object compute() throws Exception {
                prepare(var1);
                return true;
            }
        };
    }

    public double[] normalization(Candidate[][] candidates, double minimum_number_matched_peaks_losses) {
        return null;
    }
}
