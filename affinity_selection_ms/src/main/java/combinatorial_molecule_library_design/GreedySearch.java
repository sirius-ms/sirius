package combinatorial_molecule_library_design;

import java.util.ArrayList;
import java.util.BitSet;

public class GreedySearch {

    private final int[][] bbMasses;
    private final CMLEvaluator cmlEvaluator;
    private final int[][] minBBSetIndices;
    private final BitSet[] optimalBBs;
    private double optimalScore;

    public GreedySearch(int[][] bbMasses, int[][] minBBSetIndices, CMLEvaluator cmlEvaluator){
        this.bbMasses = bbMasses;
        this.cmlEvaluator = cmlEvaluator;
        this.minBBSetIndices = minBBSetIndices;

        this.optimalBBs = new BitSet[this.bbMasses.length];
        for(int i = 0; i < this.bbMasses.length; i++){
            this.optimalBBs[i] = new BitSet(this.bbMasses[i].length);
            this.optimalBBs[i].flip(0, this.bbMasses[i].length);
        }
        this.optimalScore = Double.NEGATIVE_INFINITY;
    }

    public GreedySearch(double[][] bbMasses, double blowupFactor, int[][] minBBSetIndices, CMLEvaluator cmlEvaluator){
        this(CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor), minBBSetIndices, cmlEvaluator);
    }

    public int[][] computeOptimalBBs(){
        return null;
    }

    private ArrayList<BitSet[]> getChildren(BitSet[] node){
        return null;
    }

    private int[][] bitSetArrayToBBMassMatrix(BitSet[] node){
        return null;
    }





}
