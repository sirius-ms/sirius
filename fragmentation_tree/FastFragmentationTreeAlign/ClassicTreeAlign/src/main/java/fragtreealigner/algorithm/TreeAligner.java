
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

import fragtreealigner.algorithm.ScoringFunctionNeutralLosses.ScoreWeightingType;
import fragtreealigner.domainobjects.Alignment;
import fragtreealigner.domainobjects.graphs.AlignmentResTree;
import fragtreealigner.domainobjects.graphs.AlignmentResTreeNode;
import fragtreealigner.domainobjects.graphs.AlignmentTree;
import fragtreealigner.domainobjects.graphs.AlignmentTreeNode;
import fragtreealigner.domainobjects.graphs.Graph.RearrangementType;
import fragtreealigner.util.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class TreeAligner implements Serializable {
    public enum NormalizationType { NONE, ALL, TREE_SIZE_ARITHMETIC, TREE_SIZE_GEOMETRIC, SELF_ALIG_ARITHMETIC, SELF_ALIG_GEOMETRIC, SELF_ALIGN_MIN, P_VALUE}

    private AlignmentTree tree1;
    private AlignmentTree tree2;
    private ScoringFunction scoringFunction;
    private boolean nodeUnionAllowed;
    private boolean local;
    private NormalizationType normalizationType;
    private float threshold;
    private boolean computePlikeValue;
    private boolean considerPullUps;
    private float[][] score;
    private float[][][] scoreUnion;
    private float[][][] scoreUnionNull;
    private float[][][][][] alignmentSimilarity;
    private int[][][][][][] traceMatrix;
    private Session session;
	private boolean global;
	private boolean endGapFree;

    private static int[][] subSetOrder; // = { {0}, {0, 1}, {0, 1, 2, 3}, {0, 1, 2, 4, 3, 5, 6, 7}, {0, 1, 2, 4, 8, 3, 5, 9, 6, 10, 12, 7, 11, 13, 14, 15}};
    private static int[][] subSets;
    private static int[][] subSetsSizeOne;
    private static Map<String, Double> locParamMap;
    private static Map<String, Double> scaleParamMap;

    public TreeAligner(Session session) {
        super();
        if (session.getParameters().makeVerboseOutput) System.err.println("Initializing tree aligner");
        this.session = session;
        if (subSetOrder == null) computeSubSets(15); // TODO should depend on tree size
    }

    public TreeAligner(AlignmentTree tree1, AlignmentTree tree2, ScoringFunction scoringFunction, Session session) {
        this(session);
        this.setTree1(tree1);
        this.setTree2(tree2);
        this.setScoringFunction(scoringFunction);
        this.setNodeUnionAllowed(session.getParameters().isNodeUnionAllowed);
        this.setLocal(session.getParameters().makeLocalAlignment);
        this.setGlobal(session.getParameters().makeGlobalAlignment);
        this.setEndGapFree(session.getParameters().makeEndGapFreeAlignment);
        this.setNormalizationType(session.getParameters().normalizationType);
        this.setComputePlikeValue(session.getParameters().computePlikeValue);
        this.setConsiderPullUps(session.getParameters().considerPullUps);
        this.threshold = Float.NEGATIVE_INFINITY;
        if (session.getParameters().makeVerboseOutput) System.err.println("  " + tree1.getId() +  " <-> " + tree2.getId());
    }

    public void setTree1(AlignmentTree tree1) {
        this.tree1 = tree1;
    }

    public AlignmentTree getTree1() {
        return tree1;
    }

    public void setTree2(AlignmentTree tree2) {
        this.tree2 = tree2;
    }

    public AlignmentTree getTree2() {
        return tree2;
    }

    public void setScoringFunction(ScoringFunction scoringFunction) {
        this.scoringFunction = scoringFunction;
    }

    public ScoringFunction getScoringFunction() {
        return scoringFunction;
    }

    public boolean isNodeUnionAllowed() {
        return nodeUnionAllowed;
    }

    public void setNodeUnionAllowed(boolean nodeUnionAllowed) {
        this.nodeUnionAllowed = nodeUnionAllowed;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }
    
    public boolean isGlobal() {
        return global;
    }
    
    public void setGlobal(boolean global) {
        this.global = global;
    }
    
    public boolean isEndGapFree() {
        return endGapFree;
    }
    
    public void setEndGapFree(boolean endGapFree) {
        this.endGapFree = endGapFree;
    }

    public NormalizationType getNormalizationType() {
        return normalizationType;
    }

    public void setNormalizationType(NormalizationType normalizationType) {
        this.normalizationType = normalizationType;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setComputePlikeValue(boolean computePlikeValue) {
        this.computePlikeValue = computePlikeValue;
    }

    public void setConsiderPullUps(boolean considerPullUps) {
        this.considerPullUps = considerPullUps;
    }

    public Alignment performAlignment() {
        if (session.getParameters().makeVerboseOutput) System.err.print("Starting alignment ");

        int i, j, numChildrenNode1 = 0, numChildrenNode2 = 0;
        AlignmentTreeNode node1, node2;
        score = new float[tree1.size() + 1][tree2.size() + 1];
        scoreUnion = new float[tree1.size() + 1][tree2.size() + 1][2];
        scoreUnionNull = new float[tree1.size() + 1][tree2.size() + 1][2];
        alignmentSimilarity = new float[tree1.size() + 1][tree2.size() + 1][][][];
        traceMatrix = new int[tree1.size() + 1][tree2.size() + 1][][][][];
        AlignmentTreeNode[] nodeList1 = new AlignmentTreeNode[tree1.size()];
        AlignmentTreeNode[] nodeList2 = new AlignmentTreeNode[tree2.size()];
        tree1.postOrderNodeList(nodeList1);
        tree2.postOrderNodeList(nodeList2);

        AlignmentResTree alignmentResult = new AlignmentResTree(session);

        float[] scoreMax1 = new float[tree1.size()];
        float[] scoreMax2 = new float[tree2.size()];
        if (!isLocal()) {
            Arrays.fill(scoreMax1, Float.NEGATIVE_INFINITY);
            Arrays.fill(scoreMax2, Float.NEGATIVE_INFINITY);
        }
        float sum1 = 0, sum2 = 0;
        float currentScore;

        for (i = 0; i < tree1.size(); i++) {
            node1 = (i == 0) ? null : (AlignmentTreeNode)nodeList1[i - 1];
            numChildrenNode1 = (i == 0) ? 0 : node1.numChildren();
            for (j = 0; j < tree2.size(); j++) {
                node2 = (j == 0) ? null : (AlignmentTreeNode)nodeList2[j - 1];
                numChildrenNode2 = (j == 0) ? 0 : node2.numChildren();
                score[i][j] = scoringFunction.score(node1, node2);
                currentScore = score[i][j];
                if (isNodeUnionAllowed()) {
                    scoreUnionNull[i][j][0] = scoringFunction.score(null, node1, node2);
                    scoreUnionNull[i][j][1] = scoringFunction.score(null, node2, node1);
                    if (i > 0) {
                        scoreUnion[i][j][0] = scoringFunction.score(node1, node1.getParent(), node2);
                        currentScore = Math.max(currentScore, scoreUnion[i][j][0]);
                    }
                    if (j > 0) {
                        scoreUnion[i][j][1] = scoringFunction.score(node2, node2.getParent(), node1);
                        currentScore = Math.max(currentScore, scoreUnion[i][j][1]);
                    }
                }
                scoreMax1[i] = Math.max(scoreMax1[i], currentScore);
                scoreMax2[j] = Math.max(scoreMax2[j], currentScore);
            }
        }

        for (i = 1; i < tree1.size(); i++) sum1 += scoreMax1[i];
        for (j = 1; j < tree2.size(); j++) sum2 += scoreMax2[j];

        if (Math.min(sum1, sum2) < threshold) return new Alignment(tree1, tree2, alignmentResult, Float.NaN, new ArrayList<Float>(), 0, 0, session);

        float stepSize = 0, nextOut = 0;
        if (session.getParameters().makeVerboseOutput) {
            stepSize = (float)tree1.size() / 10;
            nextOut = stepSize;
        }
        for (i = 0; i <= tree1.size(); i++) {
            if ((session.getParameters().makeVerboseOutput) && (i >= nextOut)) {
                System.err.print(".");
                nextOut = nextOut + stepSize;
            }
            node1 = (i == 0) ? null : (AlignmentTreeNode)nodeList1[i - 1];
            numChildrenNode1 = (i == 0) ? 0 : node1.numChildren();
            if (numChildrenNode1 >= subSetOrder.length){
                computeSubSets(numChildrenNode1);
            }
            for (j = 0; j <= tree2.size(); j++) {
                node2 = (j == 0) ? null : (AlignmentTreeNode)nodeList2[j - 1];
                numChildrenNode2 = (j == 0) ? 0 : node2.numChildren();
                if (numChildrenNode2 >= subSetOrder.length){
                    computeSubSets(numChildrenNode2);
                }
                alignmentSimilarity[i][j] = new float[(int)Math.pow(2, numChildrenNode1)][][];
                traceMatrix[i][j] = new int[(int)Math.pow(2, numChildrenNode1)][][][];
                for (int k: subSetOrder[numChildrenNode1]) {
                    alignmentSimilarity[i][j][k] = new float[(int)Math.pow(2, numChildrenNode2)][3];
                    traceMatrix[i][j][k] = new int[(int)Math.pow(2, numChildrenNode2)][3][3];
                    for (int l: subSetOrder[numChildrenNode2]) {
                        computeAlignmentSimilarity(node1, node2, k, l);
                    }
                }
            }
        }
        if (session.getParameters().makeVerboseOutput) System.err.println("");

        int subSetDef1 = 0, subSetDef2 = 0;
        float finalScore;
        if (local||endGapFree) {
            int k, l;
            finalScore = -1;
            node1 = null;
            node2 = null;
            int istart = (local)?0:tree1.size();
            int jstart = (local)?0:tree2.size();
            for (i = istart; i <= tree1.size(); i++) {
                for (j = jstart; j <= tree2.size(); j++) {
                    for (k = 0; k < alignmentSimilarity[i][j].length; k++) {
                        for (l = 0; l < alignmentSimilarity[i][j][k].length; l++) {
                            AlignmentTreeNode cand1 = (i == 0) ? null : (AlignmentTreeNode)nodeList1[i - 1];
                            AlignmentTreeNode cand2 = (j == 0) ? null : (AlignmentTreeNode)nodeList2[j - 1];

                            float rootScore = 0;
                            if (scoringFunction instanceof ScoringFunctionNeutralLosses){
                                if (session.getParameters().scoreRoot || session.getParameters().useNLandNodes || session.getParameters().useNodeLabels){
                                    rootScore = ((ScoringFunctionNeutralLosses) scoringFunction).score(cand1==null?null:cand1.getCompound(), cand2==null?null:cand2.getCompound());
                                }
                            }
                            if (session.getParameters().scoreWeightingType == ScoreWeightingType.NODE_WEIGHT
                                    && session.getParameters().oneNodePenalty){
                                if (cand1 != null) rootScore += cand1.getWeight();
                                if (cand2 != null) rootScore += cand2.getWeight();
                            }
                            if (alignmentSimilarity[i][j][k][l][0]+ rootScore > finalScore) {
                                finalScore = alignmentSimilarity[i][j][k][l][0]+rootScore;
                                node1 = (i == 0) ? null : (AlignmentTreeNode)nodeList1[i - 1];
                                node2 = (j == 0) ? null : (AlignmentTreeNode)nodeList2[j - 1];
                                subSetDef1 = k;
                                subSetDef2 = l;
                            }
                        }
                    }
                }
            }
        } else {
            node1 = (AlignmentTreeNode)tree1.getRoot();
            node2 = (AlignmentTreeNode)tree2.getRoot();
            float rootScore = 0;
            if (scoringFunction instanceof ScoringFunctionNeutralLosses){
                if (session.getParameters().useNLandNodes || session.getParameters().useNodeLabels){
                    rootScore = ((ScoringFunctionNeutralLosses) scoringFunction).score(node1==null?null:node1.getCompound(), node2==null?null:node2.getCompound());
                }
            }
            if (session.getParameters().scoreWeightingType == ScoreWeightingType.NODE_WEIGHT
                    && session.getParameters().oneNodePenalty){
                if (node1 != null) rootScore += node1.getWeight();
                if (node2 != null) rootScore += node2.getWeight();
            }
            subSetDef1 = (int)Math.pow(2, numChildrenNode1) - 1;
            subSetDef2 = (int)Math.pow(2, numChildrenNode2) - 1;
            finalScore = alignmentSimilarity[tree1.size()][tree2.size()][subSetDef1][subSetDef2][0]+rootScore;
        }


        if (true) return null;

        AlignmentResTreeNode root = alignmentResult.addNode("root", node1, node2, 0);
        if (session.getParameters().makeVerboseOutput) System.err.println("Starting trace back");
        traceBack(alignmentResult, root, nodeList1, nodeList2, node1, node2, subSetDef1, subSetDef2, 0);
        alignmentResult.determineRoot();

        float pLikeValue = 0;
        if (computePlikeValue) {
            AlignmentTree rearrangedTree1, rearrangedTree2;
            int numBetterHits = 0;
            int numRuns = session.getParameters().runsPlikeValue;
            for (int k = 0; k < numRuns; k++) {
                rearrangedTree1 = (AlignmentTree) tree1.rearrangeNodes(tree1.getRoot(), RearrangementType.RANDOM, k);
                rearrangedTree2 = (AlignmentTree) tree2.rearrangeNodes(tree2.getRoot(), RearrangementType.RANDOM, k + numRuns);
                TreeAligner treeAligner = new TreeAligner(rearrangedTree1, rearrangedTree2, scoringFunction, session);
                treeAligner.setNormalizationType(NormalizationType.NONE);
                treeAligner.setComputePlikeValue(false);
                Alignment alignment = treeAligner.performAlignment();
                float randScore = alignment.getScore();
                if (randScore >= finalScore) numBetterHits++;
            }
            pLikeValue = (float)numBetterHits / (float)numRuns;
        }

        int numPullUps = 0;
        if (considerPullUps) {
            AlignmentTree rearrangedTree, rearrangedTree1, rearrangedTree2, tree;
            AlignmentTreeNode node, nodeParent, treeRoot;
            Alignment bestAlignment = null;
            float bestScore = Float.NEGATIVE_INFINITY;

            int numBetterHits = 0;
            for (i = 1; i <= 2; i++) {
                tree = (i == 1) ? tree1 : tree2;
//				String pullUpNodes = tree.getId() + ":";
                for (int nodeId = 0; nodeId < tree.size(); nodeId++) {
                    rearrangedTree = tree.clone();
                    node = rearrangedTree.getNodes().get(nodeId);
                    treeRoot = rearrangedTree.getRoot();
                    nodeParent = node.getParent();
                    if (node != treeRoot && nodeParent != treeRoot) {
                        rearrangedTree.reconnect(node.getInEdge(),  nodeParent.getParent(), node);
                        rearrangedTree1 = (i == 1) ? rearrangedTree : tree1;
                        rearrangedTree2 = (i == 2) ? rearrangedTree : tree2;
                        TreeAligner treeAligner = new TreeAligner(rearrangedTree1, rearrangedTree2, scoringFunction, session);
                        treeAligner.setConsiderPullUps(false);
                        Alignment alignment = treeAligner.performAlignment();
                        alignment.setPullUpNode(node);
                        alignment.setPullUpParent(nodeParent);
                        alignment.setPullUpTree(rearrangedTree);
                        float aligScore = alignment.getScore();
                        if (aligScore > finalScore) {
                            numBetterHits++;
//							pullUpNodes += " " + node.toString() + " (" + aligScore + ")";
//							alignment.createGraphics(tree1.getId() + "_" + tree2.getId() + "_" + Integer.toString(i), node.toString());
                            if (aligScore > bestScore) {
                                bestScore = aligScore;
                                bestAlignment = alignment;
                            }
                        }
                    }
                }
//				System.err.println(pullUpNodes);
            }
            numPullUps = numBetterHits;
            if (bestScore > Float.NEGATIVE_INFINITY) return bestAlignment;
        }

        List<Float> scoreList = new ArrayList<Float>();
        if ( !normalizationType.equals(NormalizationType.NONE )) {
            if (!normalizationType.equals(NormalizationType.ALL)) scoreList.add(finalScore);
            float normalizedScore = normalizeScore(finalScore, scoreList);
            return new Alignment(tree1, tree2, alignmentResult, normalizedScore, scoreList, pLikeValue, numPullUps, session);
        } else {
            return new Alignment(tree1, tree2, alignmentResult, finalScore, scoreList, pLikeValue, numPullUps, session);
        }
    }

    private void computeAlignmentSimilarity(AlignmentTreeNode node1, AlignmentTreeNode node2, int subSetDef1, int subSetDef2) {
        int indexOfNode1 = (node1 == null) ? 0 : node1.getPostOrderPos();
        int indexOfNode2 = (node2 == null) ? 0 : node2.getPostOrderPos();

        int fullSubSet1, fullSubSet2, subSetWithoutChild1, subSetWithoutChild2;
        AlignmentTreeNode selectedChildOfNode2, selectedChildOfNode1;
        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][0] = 0;
        if ((subSetDef1 == 0) && (subSetDef2 == 0)) {
            alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] = score[0][0];
            if (isNodeUnionAllowed()) {
                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] = score[0][0];
                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] = score[0][0];
                if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] = 0;
                if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] = 0;
            }
            if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] = 0;
            return;
        }
        if (subSetDef1 == 0) {
            selectedChildOfNode2 = node2.getChildren().elementAt((int)(Math.log(subSetsSizeOne[subSetDef2][0]) / Math.log(2)));
            fullSubSet2 = (int)Math.pow(2, selectedChildOfNode2.numChildren()) - 1;
            subSetWithoutChild2 = subSetDef2 & ~subSetsSizeOne[subSetDef2][0];
            alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] =
                    score[0][selectedChildOfNode2.getPostOrderPos()] +
                            alignmentSimilarity[0][selectedChildOfNode2.getPostOrderPos()][0][fullSubSet2][0] +
                            alignmentSimilarity[0][indexOfNode2][0][subSetWithoutChild2][0];
            if (isNodeUnionAllowed()) {
                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] =
                        scoreUnionNull[indexOfNode1][selectedChildOfNode2.getPostOrderPos()][0] +
//					scoringFunction.score(null, node1, selectedChildOfNode2) + 
                                alignmentSimilarity[0][selectedChildOfNode2.getPostOrderPos()][0][fullSubSet2][0] +
                                alignmentSimilarity[0][indexOfNode2][0][subSetWithoutChild2][1];
                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] =
                        scoreUnion[0][selectedChildOfNode2.getPostOrderPos()][1] +
//					scoringFunction.score(selectedChildOfNode2, node2, null) + 
                                alignmentSimilarity[0][selectedChildOfNode2.getPostOrderPos()][0][fullSubSet2][0] +
                                alignmentSimilarity[0][indexOfNode2][0][subSetWithoutChild2][2];
                if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] = 0;
                if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] = 0;
            }
            if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] = 0;
            return;
        }
        if (subSetDef2 == 0) {
            selectedChildOfNode1 = node1.getChildren().elementAt((int)(Math.log(subSetsSizeOne[subSetDef1][0]) / Math.log(2)));
            fullSubSet1 = (int)Math.pow(2, selectedChildOfNode1.numChildren()) - 1;
            subSetWithoutChild1 = subSetDef1 & ~subSetsSizeOne[subSetDef1][0];
            alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] =
                    score[selectedChildOfNode1.getPostOrderPos()][0] +
                            alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][0][fullSubSet1][0][0] +
                            alignmentSimilarity[indexOfNode1][0][subSetWithoutChild1][0][0];
            if (isNodeUnionAllowed()) {
                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] =
                        scoreUnion[selectedChildOfNode1.getPostOrderPos()][0][0] +
//					scoringFunction.score(selectedChildOfNode1, node1, null) + 
                                alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][0][fullSubSet1][0][0] +
                                alignmentSimilarity[indexOfNode1][0][subSetWithoutChild1][0][1];
                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] =
                        scoreUnionNull[selectedChildOfNode1.getPostOrderPos()][indexOfNode2][1] +
//					scoringFunction.score(null, node2, selectedChildOfNode1) + 
                                alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][0][fullSubSet1][0][0] +
                                alignmentSimilarity[indexOfNode1][0][subSetWithoutChild1][0][2];
                if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] = 0;
                if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] = 0;
            }
            if ((local||endGapFree) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] < 0)) alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] = 0;
            return;
        }

        float max = Float.NEGATIVE_INFINITY, max2 = Float.NEGATIVE_INFINITY, max3 = Float.NEGATIVE_INFINITY;
        float maxTmp, maxTmp2, maxTmp3;
        if(local) {
            max = 0;
            max2 = 0;
            max3 = 0;
        }
        for (int i: subSetsSizeOne[subSetDef1]) {
            selectedChildOfNode1 = node1.getChildren().elementAt((int)(Math.log(i) / Math.log(2)));
            fullSubSet1 = (int)Math.pow(2, selectedChildOfNode1.numChildren()) - 1;
            subSetWithoutChild1 = subSetDef1 & ~i;
            for (int j: subSetsSizeOne[subSetDef2]) {
                selectedChildOfNode2 = node2.getChildren().elementAt((int)(Math.log(j) / Math.log(2)));
                fullSubSet2 = (int)Math.pow(2, selectedChildOfNode2.numChildren()) - 1;
                subSetWithoutChild2 = subSetDef2 & ~j;
                maxTmp =
                        score[selectedChildOfNode1.getPostOrderPos()][selectedChildOfNode2.getPostOrderPos()] +
                                alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][selectedChildOfNode2.getPostOrderPos()][fullSubSet1][fullSubSet2][0] +
                                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0];
                if (max < maxTmp) {
                    max = maxTmp;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][0] = 1;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][1] = i;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][2] = j;
                }
                if (isNodeUnionAllowed()) {
                    maxTmp2 =
                            scoreUnion[selectedChildOfNode1.getPostOrderPos()][selectedChildOfNode2.getPostOrderPos()][0] +
//						scoringFunction.score(selectedChildOfNode1, node1, selectedChildOfNode2) + 
                                    alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][selectedChildOfNode2.getPostOrderPos()][fullSubSet1][fullSubSet2][0] +
                                    alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][1];
                    maxTmp3 =
                            scoreUnion[selectedChildOfNode1.getPostOrderPos()][selectedChildOfNode2.getPostOrderPos()][1] +
//						scoringFunction.score(selectedChildOfNode2, node2, selectedChildOfNode1) + 
                                    alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][selectedChildOfNode2.getPostOrderPos()][fullSubSet1][fullSubSet2][0] +
                                    alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][2];
                    if (max2 < maxTmp2) {
                        max2 = maxTmp2;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1][0] = 1;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1][1] = i;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1][2] = j;
                    }
                    if (max3 < maxTmp3) {
                        max3 = maxTmp3;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2][0] = 1;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2][1] = i;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2][2] = j;
                    }
                }
            }
        }

        for (int i: subSetsSizeOne[subSetDef1]) {
            selectedChildOfNode1 = node1.getChildren().elementAt((int)(Math.log(i) / Math.log(2)));
            fullSubSet1 = (int)Math.pow(2, selectedChildOfNode1.numChildren()) - 1;
            subSetWithoutChild1 = subSetDef1 & ~i;
            for (int j: subSets[subSetDef2]) {
                subSetWithoutChild2 = subSetDef2 & ~j;
                maxTmp =
                        score[selectedChildOfNode1.getPostOrderPos()][0] +
                                alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][indexOfNode2][fullSubSet1][j][0] +
                                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0];
                if (max < maxTmp) {
                    max = maxTmp;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][0] = 2;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][1] = i;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][2] = j;
                }
                if (isNodeUnionAllowed() && (selectedChildOfNode1.numChildren() > 0)) {
                    maxTmp2 =
                            scoringFunction.getScoreUnion() +
                                    alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][indexOfNode2][fullSubSet1][j][1] +
                                    alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0];
                    if (max < maxTmp2) {
                        max = maxTmp2;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][0] = 4;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][1] = i;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][2] = j;
                    }
                }
            }
        }

        for (int i: subSets[subSetDef1]) {
            subSetWithoutChild1 = subSetDef1 & ~i;
            for (int j: subSetsSizeOne[subSetDef2]) {
                selectedChildOfNode2 = node2.getChildren().elementAt((int)(Math.log(j) / Math.log(2)));
                fullSubSet2 = (int)Math.pow(2, selectedChildOfNode2.numChildren()) - 1;
                subSetWithoutChild2 = subSetDef2 & ~j;
                maxTmp =
                        score[0][selectedChildOfNode2.getPostOrderPos()] +
                                alignmentSimilarity[indexOfNode1][selectedChildOfNode2.getPostOrderPos()][i][fullSubSet2][0] +
                                alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0];
                if (max < maxTmp) {
                    max = maxTmp;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][0] = 3;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][1] = i;
                    traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][2] = j;
                }
                if (isNodeUnionAllowed() && (selectedChildOfNode2.numChildren() > 0)) {
                    maxTmp3 =
                            scoringFunction.getScoreUnion() +
                                    alignmentSimilarity[indexOfNode1][selectedChildOfNode2.getPostOrderPos()][i][fullSubSet2][2] +
                                    alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0];
                    if (max < maxTmp3) {
                        max = maxTmp3;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][0] = 5;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][1] = i;
                        traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0][2] = j;
                    }
                }
            }
        }

        alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] = max;
        alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][1] = max2;
        alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][2] = max3;
//		alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2] = Math.max(max1, Math.max(max2, max3));
//		traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][0] = 0;
    }

    private void traceBack(AlignmentResTree alignmentResult, AlignmentResTreeNode parent, AlignmentTreeNode[] nodeList1, AlignmentTreeNode[] nodeList2, AlignmentTreeNode node1, AlignmentTreeNode node2, int subSetDef1, int subSetDef2, int type) {
        int indexOfNode1 = (node1 == null) ? 0 : node1.getPostOrderPos();
        int indexOfNode2 = (node2 == null) ? 0 : node2.getPostOrderPos();
        if ((local) && (alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][type] <= 0)) return;

        int fullSubSet1, fullSubSet2, subSetWithoutChild1, subSetWithoutChild2;
        AlignmentTreeNode selectedChildOfNode1, selectedChildOfNode2;
        float score = alignmentSimilarity[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][type];

        int[] trace = traceMatrix[indexOfNode1][indexOfNode2][subSetDef1][subSetDef2][type];
        AlignmentResTreeNode resNode = null;
        switch(trace[0]) {
            case 0:
                if ((subSetDef1 == 0) && (subSetDef2 == 0)) return;
                if (subSetDef2 == 0) {
                    selectedChildOfNode1 = node1.getChildren().elementAt((int)(Math.log(subSetsSizeOne[subSetDef1][0]) / Math.log(2)));
                    fullSubSet1 = (int)Math.pow(2, selectedChildOfNode1.numChildren()) - 1;
                    subSetWithoutChild1 = subSetDef1 & ~subSetsSizeOne[subSetDef1][0];

                    score -= alignmentSimilarity[indexOfNode1][0][subSetWithoutChild1][0][type] +
                            alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][0][fullSubSet1][0][0];
                    resNode = alignmentResult.addNode(Integer.toString(alignmentResult.size()), selectedChildOfNode1, null, score);
                    traceBack(alignmentResult, parent, nodeList1, nodeList2, node1, null, subSetWithoutChild1, 0, type);
                    traceBack(alignmentResult, resNode, nodeList1, nodeList2, selectedChildOfNode1, null, fullSubSet1, 0, 0);
                }
                if (subSetDef1 == 0) {
                    selectedChildOfNode2 = node2.getChildren().elementAt((int)(Math.log(subSetsSizeOne[subSetDef2][0]) / Math.log(2)));
                    fullSubSet2 = (int)Math.pow(2, selectedChildOfNode2.numChildren()) - 1;
                    subSetWithoutChild2 = subSetDef2 & ~subSetsSizeOne[subSetDef2][0];

                    score -= alignmentSimilarity[0][indexOfNode2][0][subSetWithoutChild2][type] +
                            alignmentSimilarity[0][selectedChildOfNode2.getPostOrderPos()][0][fullSubSet2][0];
                    resNode = alignmentResult.addNode(Integer.toString(alignmentResult.size()), null, selectedChildOfNode2, score);
                    traceBack(alignmentResult, parent, nodeList1, nodeList2, null, node2, 0, subSetWithoutChild2, type);
                    traceBack(alignmentResult, resNode, nodeList1, nodeList2, null, selectedChildOfNode2, 0, fullSubSet2, 0);
                }
                break;

            case 1:
                selectedChildOfNode1 = node1.getChildren().elementAt((int)(Math.log(trace[1]) / Math.log(2)));
                selectedChildOfNode2 = node2.getChildren().elementAt((int)(Math.log(trace[2]) / Math.log(2)));
                subSetWithoutChild1 = subSetDef1 & ~trace[1];
                subSetWithoutChild2 = subSetDef2 & ~trace[2];
                fullSubSet1 = (int)Math.pow(2, selectedChildOfNode1.numChildren()) - 1;
                fullSubSet2 = (int)Math.pow(2, selectedChildOfNode2.numChildren()) - 1;

                score -= alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][type] +
                        alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][selectedChildOfNode2.getPostOrderPos()][fullSubSet1][fullSubSet2][0];
                resNode = alignmentResult.addNode(Integer.toString(alignmentResult.size()), selectedChildOfNode1, selectedChildOfNode2, score);
                traceBack(alignmentResult, parent, nodeList1, nodeList2, node1, node2, subSetWithoutChild1, subSetWithoutChild2, type);
                traceBack(alignmentResult, resNode, nodeList1, nodeList2, selectedChildOfNode1,	selectedChildOfNode2, fullSubSet1, fullSubSet2, 0);
                break;

            case 2:
                selectedChildOfNode1 = node1.getChildren().elementAt((int)(Math.log(trace[1]) / Math.log(2)));
                subSetWithoutChild1 = subSetDef1 & ~trace[1];
                subSetWithoutChild2 = subSetDef2 & ~trace[2];
                fullSubSet1 = (int)Math.pow(2, selectedChildOfNode1.numChildren()) - 1;

                score -= alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0] +
                        alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][indexOfNode2][fullSubSet1][trace[2]][0];
                resNode = alignmentResult.addNode(Integer.toString(alignmentResult.size()), selectedChildOfNode1, null, score);
                traceBack(alignmentResult, parent, nodeList1, nodeList2, node1, node2, subSetWithoutChild1, subSetWithoutChild2, 0);
                traceBack(alignmentResult, resNode, nodeList1, nodeList2, selectedChildOfNode1,	node2, fullSubSet1, trace[2], 0);
                break;

            case 3:
                selectedChildOfNode2 = node2.getChildren().elementAt((int)(Math.log(trace[2]) / Math.log(2)));
                subSetWithoutChild1 = subSetDef1 & ~trace[1];
                subSetWithoutChild2 = subSetDef2 & ~trace[2];
                fullSubSet2 = (int)Math.pow(2, selectedChildOfNode2.numChildren()) - 1;

                score -= alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0] +
                        alignmentSimilarity[indexOfNode1][selectedChildOfNode2.getPostOrderPos()][trace[1]][fullSubSet2][0];
                resNode = alignmentResult.addNode(Integer.toString(alignmentResult.size()), null, selectedChildOfNode2, score);
                traceBack(alignmentResult, parent, nodeList1, nodeList2, node1, node2, subSetWithoutChild1, subSetWithoutChild2, 0);
                traceBack(alignmentResult, resNode, nodeList1, nodeList2, node1, selectedChildOfNode2, trace[1], fullSubSet2, 0);
                break;

            case 4:
                selectedChildOfNode1 = node1.getChildren().elementAt((int)(Math.log(trace[1]) / Math.log(2)));
                subSetWithoutChild1 = subSetDef1 & ~trace[1];
                subSetWithoutChild2 = subSetDef2 & ~trace[2];
                fullSubSet1 = (int)Math.pow(2, selectedChildOfNode1.numChildren()) - 1;

                score -= alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0] +
                        alignmentSimilarity[selectedChildOfNode1.getPostOrderPos()][indexOfNode2][fullSubSet1][trace[2]][1];
                resNode = alignmentResult.addNode(Integer.toString(alignmentResult.size()), selectedChildOfNode1, null, score, 'c');
                traceBack(alignmentResult, parent, nodeList1, nodeList2, node1, node2, subSetWithoutChild1, subSetWithoutChild2, 0);
                traceBack(alignmentResult, resNode, nodeList1, nodeList2, selectedChildOfNode1,	node2, fullSubSet1, trace[2], 1);
                break;

            case 5:
                selectedChildOfNode2 = node2.getChildren().elementAt((int)(Math.log(trace[2]) / Math.log(2)));
                subSetWithoutChild1 = subSetDef1 & ~trace[1];
                subSetWithoutChild2 = subSetDef2 & ~trace[2];
                fullSubSet2 = (int)Math.pow(2, selectedChildOfNode2.numChildren()) - 1;

                score -= alignmentSimilarity[indexOfNode1][indexOfNode2][subSetWithoutChild1][subSetWithoutChild2][0] +
                        alignmentSimilarity[indexOfNode1][selectedChildOfNode2.getPostOrderPos()][trace[1]][fullSubSet2][2];
                resNode = alignmentResult.addNode(Integer.toString(alignmentResult.size()), null, selectedChildOfNode2, score, 'c');
                traceBack(alignmentResult, parent, nodeList1, nodeList2, node1, node2, subSetWithoutChild1, subSetWithoutChild2, 0);
                traceBack(alignmentResult, resNode, nodeList1, nodeList2, node1, selectedChildOfNode2, trace[1], fullSubSet2, 2);
                break;
        }
//		TODO Score hinzufuegen
        alignmentResult.connect(parent, resNode);
    }

    private float normalizeScore(float score, List<Float> scoreList) {
        switch (normalizationType) {
            case NONE:
                return score;
            case TREE_SIZE_ARITHMETIC:
                return (float)(score / ((float)(tree1.size() + tree2.size()) / 2) * 10);
            case TREE_SIZE_GEOMETRIC:
                return (float)(score / (Math.sqrt(tree1.size()) * Math.sqrt(tree2.size())) * 10);
            case SELF_ALIG_ARITHMETIC:
            case SELF_ALIG_GEOMETRIC:
            case SELF_ALIGN_MIN:
                float selfAligScore1, selfAligScore2;
                if (tree1.getSelfAligScore() != 0) {
                    selfAligScore1 = tree1.getSelfAligScore();
                } else {
                    TreeAligner treeAligner = new TreeAligner(tree1, tree1, scoringFunction, session);
                    treeAligner.setLocal(false);
                    treeAligner.setNormalizationType(NormalizationType.NONE);
                    selfAligScore1 = treeAligner.performAlignment().getScore();
                    tree1.setSelfAligScore(selfAligScore1);
                }
                if (tree2.getSelfAligScore() != 0) {
                    selfAligScore2 = tree2.getSelfAligScore();
                } else {
                    TreeAligner treeAligner = new TreeAligner(tree2, tree2, scoringFunction, session);
                    treeAligner.setLocal(false);
                    treeAligner.setNormalizationType(NormalizationType.NONE);
                    selfAligScore2 = treeAligner.performAlignment().getScore();
                    tree2.setSelfAligScore(selfAligScore2);
                }
                if (normalizationType.equals(NormalizationType.SELF_ALIG_ARITHMETIC)) {
                    return (float)(score / ((selfAligScore1 + selfAligScore2) / 2) * 100);
                } else if (normalizationType.equals(NormalizationType.SELF_ALIGN_MIN)) {
                    if (selfAligScore1 < 0 || selfAligScore2 < 0){
                        System.err.println("normalizeScore: "+tree1.getId()+" "+selfAligScore1+" "+tree2.getId()+" "+selfAligScore2);
                    }
                    return (float)(score / Math.sqrt(Math.min(selfAligScore1, selfAligScore2)));
                } else {
                    return (float)(score / (Math.sqrt(selfAligScore1) * Math.sqrt(selfAligScore2)) * 100);
                }
            case P_VALUE:
                int min = Math.min(tree1.size(), tree2.size());
                int max = Math.max(tree1.size(), tree2.size());
                Double location = locParamMap.get(min+"x"+max);
                if (location== null){
                    return Float.NEGATIVE_INFINITY;
                }
                Double scale = scaleParamMap.get(min+"x"+max);
                double result =  cumulativeGumbelComplement(score, location, scale);
                if (result == 0){
                    System.err.println("Warning: log(0) used in p-Val calculation of"+tree1.getId()+" "+tree2.getId());
                }
                return (float) (-1*Math.log(result));
            case ALL:
                normalizationType = NormalizationType.SELF_ALIG_ARITHMETIC;
                scoreList.add(normalizeScore(score, null));
                normalizationType = NormalizationType.SELF_ALIG_GEOMETRIC;
                scoreList.add(normalizeScore(score, null));
                normalizationType = NormalizationType.TREE_SIZE_ARITHMETIC;
                scoreList.add(normalizeScore(score, null));
                normalizationType = NormalizationType.TREE_SIZE_GEOMETRIC;
                scoreList.add(normalizeScore(score, null));
                normalizationType = NormalizationType.SELF_ALIGN_MIN;
                scoreList.add(normalizeScore(score, null));
                normalizationType = NormalizationType.ALL;
                return score;
            default:
                System.err.println("Unknown normalization type!");
                return 0;
        }
    }

    private static void computeSubSets(int maxDegree) {
        int k, l;

        subSetOrder = new int[maxDegree + 1][];
        for (int deg = 0; deg <= maxDegree; deg++) {
            subSetOrder[deg] = new int[(int)Math.pow(2, deg)];
            k = 0;
            for (int i = 0; i <= deg; i++) {
                for (int j = 0; j < (int)Math.pow(2, deg); j++) {
                    if (Integer.bitCount(j) == i) {
                        subSetOrder[deg][k] = j;
                        k++;
                    }
                }
            }
        }

        subSets = new int[(int)Math.pow(2, maxDegree)][];
        subSetsSizeOne = new int[(int)Math.pow(2, maxDegree)][];

        for (int i = 0; i < Math.pow(2, maxDegree); i++) {
            subSets[i] = new int[(int)Math.pow(2, Integer.bitCount(i))];
            subSets[i][0] = 0;
            subSetsSizeOne[i] = new int[Integer.bitCount(i)];
            k = 1;
            l = 0;
            for (int j = 1; j <= i; j++) {
                if ((j | i) == i) {
                    subSets[i][k++] = j;
                    if (Integer.bitCount(j) == 1) subSetsSizeOne[i][l++] = j; //(int)(Math.log(j) / Math.log(2));
                }
            }
        }
    }


    public static void readStatisticalParameter(BufferedReader r) throws IOException {
        locParamMap = new HashMap<String, Double>();
        scaleParamMap = new HashMap<String, Double>();
        for (String line = r.readLine(); line != null; line= r.readLine()){
            String[] arr = line.split(" ");
            String[] treeSizes = arr[0].split("x");
            // Use statitistic only if confidence > 5% or smaller tree > 10
            if (arr.length > 3 /*&& (Double.parseDouble(arr[3])>0.05 || Integer.parseInt(treeSizes[0]) > 10)*/){
                locParamMap.put(arr[0], Double.parseDouble(arr[1]));
                scaleParamMap.put(arr[0], Double.parseDouble(arr[2]));
            }
        }
    }

    private static double cumulativeGumbelComplement(double x, double loc, double scale){
        double exponent = (loc-x)/scale;
        return expComplement(-1*Math.exp(exponent));
    }

    // This calculates 1-exp(x) by its Taylor Series
    // Avoiding underflow around exp(0)
    // but should not be used with x < -2
    private static double expComplement(double x){
        if (x < -2 || x > 10){
            return 1-Math.exp(x);
        }
        double res = 0;
        int factorial = 1;
        for (int i = 1; i <= 10; ++i){
            factorial *= i;
            res -= Math.pow(x,i)/factorial;
        }
        return res;
    }

    public Alignment performSelfAlignment() {
        Float finalScore = tree1.getSelfAligScore();
        List<Float> scoreList = new ArrayList<Float>();
        if ( !normalizationType.equals(NormalizationType.NONE )) {
            if (!normalizationType.equals(NormalizationType.ALL)) scoreList.add(finalScore);
            float normalizedScore = normalizeScore(finalScore, scoreList);
            return new Alignment(tree1, tree2, null, normalizedScore, scoreList, 0, 0, session);
        } else {
            return new Alignment(tree1, tree2, null, finalScore, scoreList, 0, 0, session);
        }
    }
}
