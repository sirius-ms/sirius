package combinatorial_molecule_library_design;

import java.util.BitSet;

public class OptimalMoleculeLibraryCalculator {

    private final double[][] bbMasses;
    private final double blowupFactor;
    private final BitSet[] currentBuildingBlockSubsets;
    private double entropy;

    public OptimalMoleculeLibraryCalculator(double[][] bbMasses, double blowupFactor){   // sorted bbMasses at each position
        // Initialisation:
        this.bbMasses = bbMasses;
        this.blowupFactor = blowupFactor;
        this.currentBuildingBlockSubsets = new BitSet[bbMasses.length];
        this.entropy = 0;

        // Start point is the library created by only one building block for each position (containing one molecule).
        // The entropy for this library is 0.
        for(int i = 0; i < bbMasses.length; i++){
            this.currentBuildingBlockSubsets[i] = new BitSet(bbMasses[i].length);
            this.currentBuildingBlockSubsets[i].set(0);
        }
    }

    public void computeOptimalBuildingBlockSubsets(){

    }

    private double[][] getBBMassSubsets(BitSet[] subsets){
        double[][] bbMassSubsets = new double[this.bbMasses.length][];
        for(int idx = 0; idx < this.bbMasses.length; idx++){
            bbMassSubsets[idx] = this.getBBMassSubset(subsets[idx], idx);
        }
        return bbMassSubsets;
    }

    private double[] getBBMassSubset(BitSet subset, int idx){
        double[] bbMassSubset = new double[subset.cardinality()];
        int j = 0;
        for(int i = subset.nextSetBit(0); i >= 0; i = subset.nextSetBit(i+1)){
            bbMassSubset[j] = this.bbMasses[idx][i];
            j++;
        }
        return bbMassSubset;
    }

    private void incrementBitSet(BitSet bitset){
        int idx = 0;
        while(bitset.get(idx)){
            bitset.set(idx, false);
            idx++;
        }
        bitset.set(idx, true);
    }

}
