package de.unijena.bioinf.cmlDesign;

import java.util.ArrayList;
import java.util.BitSet;

public class GreedySearch {

    private final int[][] bbMasses;
    private final CMLEvaluator cmlEvaluator;
    private final int[][] minBBSetIndices; // contains in each row an array of SORTED bb-indices which cannot be removed!
    private BitSet[] optimalBBs;
    private double optimalScore;

    public GreedySearch(int[][] bbMasses, int[][] minBBSetIndices, CMLEvaluator cmlEvaluator){
        this.bbMasses = bbMasses;
        this.cmlEvaluator = cmlEvaluator;
        this.minBBSetIndices = minBBSetIndices;
        this.optimalScore = Double.NEGATIVE_INFINITY;
    }

    public GreedySearch(double[][] bbMasses, double blowupFactor, int[][] minBBSetIndices, CMLEvaluator cmlEvaluator){
        this(CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor), minBBSetIndices, cmlEvaluator);
    }

    public int[][] computeOptimalBBs(){
        // INIT:
        // The starting point for the greedy search heuristic is the root representing all building blocks.
        this.optimalBBs = new BitSet[this.bbMasses.length];
        for(int i = 0; i < this.bbMasses.length; i++){
            this.optimalBBs[i] = new BitSet(this.bbMasses[i].length);
            this.optimalBBs[i].flip(0, this.bbMasses[i].length);
        }
        this.optimalScore = this.cmlEvaluator.evaluate(this.bbMasses);

        // Now: traverse through the search graph as long as there is a direct child with better score
        boolean isNewSolution = true;
        while(isNewSolution){
            isNewSolution = false;
            ArrayList<BitSet[]> children = this.getChildren(this.optimalBBs);
            for(BitSet[] child : children){
                double score = this.evaluateNode(child);
                if(score > this.optimalScore){
                    this.optimalScore = score;
                    this.optimalBBs = child;
                    isNewSolution = true;
                }
            }
        }

        return this.bitSetArrayToBBMassMatrix(this.optimalBBs);
    }

    private double evaluateNode(BitSet[] node){
        int[][] bbMassesSubset = this.bitSetArrayToBBMassMatrix(node);
        return this.cmlEvaluator.evaluate(bbMassesSubset);
    }

    private ArrayList<BitSet[]> getChildren(BitSet[] node){
        ArrayList<BitSet[]> children = new ArrayList<>();
        for(int i = 0; i < node.length; i++){ // node.length = this.bbMasses.length
            if(node[i].cardinality() > 1) {
                int k = 0;
                for(int j = node[i].nextSetBit(0); j >= 0; j = node[i].nextSetBit(j + 1)) {
                    if(this.minBBSetIndices[i].length == 0 || j != this.minBBSetIndices[i][k]){ // Is j the index of an essential bb?
                        BitSet[] child = this.cloneBitSetArray(node);
                        child[i].set(j, false);
                        children.add(child);
                    }else{
                        k = k < this.minBBSetIndices[i].length - 1 ? k+1 : k;
                    }
                }
            }
        }
        return children;
    }

    private BitSet[] cloneBitSetArray(BitSet[] bitSets){
        BitSet[] clonedBitSets = new BitSet[bitSets.length];
        for(int idx = 0; idx < bitSets.length; idx++) clonedBitSets[idx] = (BitSet) bitSets[idx].clone();
        return clonedBitSets;
    }

    private int[][] bitSetArrayToBBMassMatrix(BitSet[] node){
        int[][] bbMassesSubset = new int[this.bbMasses.length][];
        for(int i = 0; i < this.bbMasses.length; i++){
            bbMassesSubset[i] = new int[node[i].cardinality()];
            int k = 0;
            for(int j = node[i].nextSetBit(0); j >= 0; j = node[i].nextSetBit(j+1)){
                bbMassesSubset[i][k] = this.bbMasses[i][j];
                k++;
            }
        }
        return bbMassesSubset;
    }

    public int[][] getOptimalBBMasses(){
        return this.bitSetArrayToBBMassMatrix(this.optimalBBs);
    }

    public BitSet[] getOptimalBBBitSets(){
        return this.optimalBBs;
    }

    public double getOptimalScore(){
        return this.optimalScore;
    }
}
