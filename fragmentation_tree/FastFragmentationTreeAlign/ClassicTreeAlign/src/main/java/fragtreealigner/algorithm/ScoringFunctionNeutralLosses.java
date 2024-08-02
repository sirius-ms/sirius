
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package fragtreealigner.algorithm;

import fragtreealigner.domainobjects.chem.components.Compound;
import fragtreealigner.domainobjects.chem.components.NeutralLoss;
import fragtreealigner.domainobjects.db.FragmentationTreeDatabase;
import fragtreealigner.domainobjects.graphs.AlignmentTreeNode;
import fragtreealigner.util.Parameters;
import fragtreealigner.util.Session;

@SuppressWarnings("serial")
public class ScoringFunctionNeutralLosses extends ScoringFunction {
    public enum ScoreWeightingType { NONE, NODE_WEIGHT, NEUTRAL_LOSS_FREQUENCY }

    private float scoreEquality;
    private float scoreNodeEquality;
    private float scoreNodeInequality;
    private float scoreNodeEqualityPerAtom;
    private float scoreNodeInequalityPerAtom;
    private float scoreEqualityPerAtom;
    private float scoreInequality;
    private float scoreInequalityPerAtom;
    private float scoreGap;
    private float scoreGapCnl;
    private float scoreCnlCnl;
    private float scoreDiffCommonFirstOrder;
    private float scoreDiffCommonSecondOrder;
    private float scoreDiffCommonPerAtom;
    private float scoreDiffH2;
    private float scoreDiffH2PerAtom;
    private float scoreDiffCommonPlusH2;
    private float scoreDiffCommonPlusH2PerAtom;
    private float scoreSimilarNLMass;
    private ScoreWeightingType scoreWeightingType;
    private FragmentationTreeDatabase db;

    public ScoringFunctionNeutralLosses() {
        super();
    }

    public ScoringFunctionNeutralLosses(Session session) {
        super(session);
    }

    public ScoringFunctionNeutralLosses(FragmentationTreeDatabase db, Session session) {
        this(session);
        this.db = db;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (session == null) {
            scoreEquality                =  5;
            scoreEqualityPerAtom         =  1;
            scoreInequality              = -5;
            scoreInequalityPerAtom       = -1;
            scoreGap                     = -8;
            scoreGapCnl                  = -2;
            scoreCnlCnl                  =  2;
            scoreDiffCommonFirstOrder	 =  2;
            scoreDiffCommonSecondOrder	 =  0;
            scoreDiffCommonPerAtom       =  0.5f;
            scoreDiffH2                  =  3;
            scoreDiffH2PerAtom           =  1;
            scoreDiffCommonPlusH2        =  0;
            scoreDiffCommonPlusH2PerAtom =  0.1f;
            scoreSimilarNLMass           =  3f;
            scoreWeightingType           = ScoreWeightingType.NONE;
        } else {
            Parameters parameters        = session.getParameters();
            scoreEquality                = parameters.scoreEquality;
            scoreNodeEquality            = parameters.scoreNodeEquality;
            scoreNodeInequality          = parameters.scoreNodeInequality;
            scoreNodeEqualityPerAtom     = parameters.scoreNodeEqualityPerAtom;
            scoreNodeInequalityPerAtom   = parameters.scoreNodeInequalityPerAtom;
            scoreEqualityPerAtom         = parameters.scoreEqualityPerAtom;
            scoreInequality              = parameters.scoreInequality;
            scoreInequalityPerAtom       = parameters.scoreInequalityPerAtom;
            scoreGap                     = parameters.scoreGap;
            scoreGapCnl                  = parameters.scoreGapCnl;
            scoreCnlCnl                  = parameters.scoreCnlCnl;
            scoreDiffCommonFirstOrder    = parameters.scoreDiffCommonFirstOrder;
            scoreDiffCommonSecondOrder   = parameters.scoreDiffCommonSecondOrder;
            scoreDiffCommonPerAtom       = parameters.scoreDiffCommonPerAtom;
            scoreDiffH2                  = parameters.scoreDiffH2;
            scoreDiffH2PerAtom           = parameters.scoreDiffH2PerAtom;
            scoreDiffCommonPlusH2        = parameters.scoreDiffCommonPlusH2;
            scoreDiffCommonPlusH2PerAtom = parameters.scoreDiffCommonPlusH2PerAtom;
            scoreSimilarNLMass           = parameters.scoreSimilarNLMass;
            scoreWeightingType           = parameters.scoreWeightingType;
        }
    }

    @Override
    public float score(AlignmentTreeNode node1, AlignmentTreeNode node2) {
        if ((node1 == null) && (node2 == null)) return scoreNullNull;
        NeutralLoss neutralLoss1 = (node1 == null) ? null : node1.getNeutralLoss();
        NeutralLoss neutralLoss2 = (node2 == null) ? null : node2.getNeutralLoss();
        Compound c1 = (node1 == null) ? null : node1.getCompound();
        Compound c2 = (node2 == null) ? null : node2.getCompound();

        float score = 0;
        if (!session.getParameters().useNodeLabels){
            // scoring on neutral losses
            score += score(neutralLoss1, neutralLoss2);
        }
        if (session.getParameters().useNLandNodes || session.getParameters().useNodeLabels) {
            //scoring on peak explanations
            score += score(c1, c2);
        }

        if (!scoreWeightingType.equals(ScoreWeightingType.NONE)) {
            double weight1 = 0, weight2 = 0;
            if (scoreWeightingType.equals(ScoreWeightingType.NODE_WEIGHT)) {
                weight1 = (node1 == null) ? node2.getWeight() : node1.getWeight();
                weight2 = (node2 == null) ? node1.getWeight() : node2.getWeight();
//				return (float)(weight1 * weight2 * score);
//				return (float)(((Math.log(weight1 + 2.7)) * (Math.log(weight2 + 2.7))) * score);
//				if (weight1 < 0) weight1 = 0;
//				if (weight2 < 0) weight2 = 0;
//				return (float)(((Math.log(weight1) + 20) * (Math.log(weight2) + 20) / 1000) * score);
                // original code by Thomas:
//				return (float)(weight1 * weight2 * score);
                // approach for manipulators
                return (float)(score +(weight1+weight2)*Math.signum(score));
            } else if (scoreWeightingType.equals(ScoreWeightingType.NEUTRAL_LOSS_FREQUENCY) && db != null) {
                float neutralLoss1Count = db.getDatabaseStatistics().getNeutralLossCount(neutralLoss1);
                float neutralLoss2Count = db.getDatabaseStatistics().getNeutralLossCount(neutralLoss2);
                if (neutralLoss1Count == 0) neutralLoss1Count = 1;
                if (neutralLoss2Count == 0) neutralLoss2Count = 1;
                neutralLoss1Count = (neutralLoss1Count / (float) db.getDatabaseStatistics().getMaxNeutralLossCount()) * 3 + 1;
                neutralLoss2Count = (neutralLoss2Count / (float) db.getDatabaseStatistics().getMaxNeutralLossCount()) * 3 + 1;
                if (node1 != null) weight1 = 1.0 / neutralLoss1Count;
                else weight1 = 1.0 / neutralLoss2Count;
                if (node2 != null) weight2 = 1.0 / neutralLoss2Count;
                else weight2 = 1.0 / neutralLoss1Count;
                return (float)(weight1 * weight2 * score);
            }
            else return score;
        }
        else return score;
    }

    @Override
    public float score(AlignmentTreeNode node1p1, AlignmentTreeNode node1p2, AlignmentTreeNode node2) {
        if ((node1p1 == null) || (node1p2 == null)) return Float.NEGATIVE_INFINITY;
        else if ((node1p1.getNeutralLoss() == null) || (node1p2.getNeutralLoss() == null)) return Float.NEGATIVE_INFINITY;

        NeutralLoss neutralLoss1 = new NeutralLoss("", 0.0, node1p2.getNeutralLoss().getMolecularFormula().add(node1p1.getNeutralLoss().getMolecularFormula()), session);
        NeutralLoss neutralLoss2 = (node2 == null) ? null : node2.getNeutralLoss();
        Compound c1 = new Compound("", 0.0, node1p2.getCompound().getMolecularFormula().add(node1p1.getCompound().getMolecularFormula()), session);
        Compound c2 = (node2 == null) ? null : node2.getCompound();

        float score = 0;
        if (!session.getParameters().useNodeLabels){
            score += score(neutralLoss1, neutralLoss2);
        }
        if (session.getParameters().useNLandNodes || session.getParameters().useNodeLabels) {
            score += score(c1, c2);
        }
        if (!scoreWeightingType.equals(ScoreWeightingType.NONE)) {
            double weight1 = 0, weight2 = 0;
            if (scoreWeightingType.equals(ScoreWeightingType.NODE_WEIGHT)) {
                weight1 = (node1p1.getWeight() + node1p2.getWeight()) / 2;
                weight2 = (node2 == null) ? weight1 : node2.getWeight();
//				return (float)(weight1 * weight2 * score);
//				if (weight1 < 0) weight1 = 0;
//				if (weight2 < 0) weight2 = 0;
//				return (float)(((Math.log(weight1) + 20) * (Math.log(weight2) + 20) / 1000) * score);
                // original code by Thomas:
//				return (float)(weight1 * weight2 * score);
                // approach for manipulators
                return (float)(score +(weight1+weight2)*Math.signum(score));
            } else if (scoreWeightingType.equals(ScoreWeightingType.NEUTRAL_LOSS_FREQUENCY)) {
                float neutralLoss1Count = db.getDatabaseStatistics().getNeutralLossCount(neutralLoss1);
                float neutralLoss2Count = db.getDatabaseStatistics().getNeutralLossCount(neutralLoss2);
                if (neutralLoss1Count == 0) neutralLoss1Count = 1;
                if (neutralLoss2Count == 0) neutralLoss2Count = 1;
                neutralLoss1Count = (neutralLoss1Count / (float) db.getDatabaseStatistics().getMaxNeutralLossCount()) * 3 + 1;
                neutralLoss2Count = (neutralLoss2Count / (float) db.getDatabaseStatistics().getMaxNeutralLossCount()) * 3 + 1;
                weight1 = 1.0 / neutralLoss1Count;
                if (node2 != null) weight2 = 1.0 / neutralLoss2Count;
                else weight2 = weight1;
                return (float)(weight1 * weight2 * score);
            } else return score;
        }
        else return score;
    }

//	private float score(NeutralLoss neutralLoss1, NeutralLoss neutralLoss2) {
//		if ((neutralLoss1 == null) && (neutralLoss2 == null)) return scoreNullNull;
//		if ((neutralLoss1 == null) || (neutralLoss2 == null)) {
//			return scoreGap;
//		}
//		if (neutralLoss1.equals(neutralLoss2)) return scoreEquality + (scoreEqualityPerAtom * neutralLoss1.size());
//		int symmDiff = neutralLoss1.symmetricDifference(neutralLoss2);
//		return scoreInequality + (scoreInequalityPerAtom * symmDiff);
//	}

    // Complex function by Thomas uses H2Diff even if everything else is disabled.
    private float score(NeutralLoss neutralLoss1, NeutralLoss neutralLoss2) {
        if ((neutralLoss1 == null) && (neutralLoss2 == null)) return scoreNullNull;
        if ((neutralLoss1 == null) || (neutralLoss2 == null)) {
            //if ((neutralLoss1 != null) && (neutralLoss1.isCommon())) return scoreGapCnl;
            //if ((neutralLoss2 != null) && (neutralLoss2.isCommon())) return scoreGapCnl;
            return scoreGap;
        }

        if (neutralLoss1.isCommon() && neutralLoss1.equals(neutralLoss2)){
            if (session.getParameters().cnlSizeDependent){
                return scoreCnlCnl+(scoreEqualityPerAtom*neutralLoss1.size());
            } else {
                return scoreCnlCnl;
            }
        }
        if (neutralLoss1.equals(neutralLoss2)) return scoreEquality + (scoreEqualityPerAtom * neutralLoss1.size());
       // if (neutralLoss1.equals(neutralLoss2)) return Math.min(scoreEquality + (scoreEqualityPerAtom * neutralLoss1.size()),20);


        int symmDiff = neutralLoss1.symmetricDifference(neutralLoss2);
        switch (session.getParameters().chemInfo.determineNeutralLossDiff(neutralLoss1, neutralLoss2)) {
            case H2:
                return scoreDiffH2 + (scoreDiffH2PerAtom * symmDiff);

            case FUNCTIONAL_GROUP_FIRST_ORDER:
                return scoreDiffCommonFirstOrder + (scoreDiffCommonPerAtom * symmDiff);
                
            case FUNCTIONAL_GROUP_SECOND_ORDER:
                return scoreDiffCommonSecondOrder + (scoreDiffCommonPerAtom * symmDiff);

            case FUNCTIONAL_GROUP_PLUS_H2:
                return scoreDiffCommonPlusH2 + (scoreDiffCommonPlusH2PerAtom * symmDiff);

            case NOT_COMMON:
                return scoreInequality + (scoreInequalityPerAtom * symmDiff);
                //return scoreInequality;// + (scoreInequalityPerAtom * symmDiff);


            case SIMILAR_NEUTRALLOSS_MASS:
                return scoreSimilarNLMass;
        }

        return 0;
    }

    public float score(Compound c1, Compound c2) {

        if ((c1 == null) && (c2 == null)) return scoreNullNull;
        if ((c1 == null) || (c2 == null)) {
            if (!session.getParameters().useOnlyNodeBonus){

                return scoreGap;

            } else return 0;
        }
        // use only bonus for nodes
        //if (c1.equals(c2)) return scoreNodeEquality;// + (scoreEqualityPerAtom * c1.size());
        if (c1.equals(c2)) return scoreNodeEquality + (scoreNodeEqualityPerAtom * c1.size());

        if (!session.getParameters().useOnlyNodeBonus){
            int symmDiff = c1.symmetricDifference(c2);
            return scoreNodeInequality + (scoreNodeInequalityPerAtom * symmDiff);
           // return scoreInequality;// + (scoreInequalityPerAtom * symmDiff);

        }
        return 0;
    }

}
