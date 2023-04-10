package combinatorial_molecule_library_design;

import java.util.BitSet;

public class OptimalMoleculeLibraryCalculator {

    private final double[][] bbMasses;
    private final double blowupFactor;
    private final double binSize;
    private final BitSet[] optimalBuildingBlockSubsets;
    private double entropy;

    public OptimalMoleculeLibraryCalculator(double[][] bbMasses, double blowupFactor, double binSize){   // sorted bbMasses at each position
        // Initialisation:
        this.bbMasses = bbMasses;
        this.blowupFactor = blowupFactor;
        this.binSize = binSize;
        this.optimalBuildingBlockSubsets = new BitSet[bbMasses.length];
        this.entropy = 0;

        // Start point is the library created by only one building block for each position (containing one molecule).
        // The entropy for this library is 0.
        for(int i = 0; i < bbMasses.length; i++){
            this.optimalBuildingBlockSubsets[i] = new BitSet(bbMasses[i].length);
            this.optimalBuildingBlockSubsets[i].set(0);
        }
    }

    public void computeOptimalBuildingBlockSubsets(){
        // Idea:
        // - for each combination of building block subsets, compute the entropy of the molecule library
        // - during this method, save the best combination of BB subsets which has the current best entropy
        // INITIALISATION:
        BitSet[] currentBuildingBlockSubsets = new BitSet[this.bbMasses.length];
        for(int i = 0; i < this.bbMasses.length; i++) currentBuildingBlockSubsets[i].set(0);

        // LOOP:
        while(this.nextCombination(currentBuildingBlockSubsets)){
            // Compute the entropy for the current combination of building block subsets:
            double[][] currentBBMasses = this.getBBMassSubsets(currentBuildingBlockSubsets);
            BinEntropyCalculator entropyCalc = new BinEntropyCalculator(currentBBMasses, this.blowupFactor, this.binSize);
            double currentEntropy = entropyCalc.computeEntropy();

            // Is this entropy currently the maximal one?
            // If so, then update this.entropy and this.optimalBuildingBlockSubsets
            if(this.entropy < currentEntropy){
                this.entropy = currentEntropy;
                for(int idx = 0; idx < this.bbMasses.length; idx++){
                    this.optimalBuildingBlockSubsets[idx] = (BitSet) currentBuildingBlockSubsets[idx].clone();
                }
            }
        }
    }

    private boolean nextCombination(BitSet[] currentBuildingBlockCombination){
        int idx = 0;
        while(idx < this.bbMasses.length){
            if(currentBuildingBlockCombination[idx].cardinality() == this.bbMasses[idx].length) {
                currentBuildingBlockCombination[idx].clear(1, this.bbMasses[idx].length);
                idx++;
            }else{
                this.incrementBitSet(currentBuildingBlockCombination[idx]);
                break;
            }
        }
        return idx < this.bbMasses.length;
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

    public BitSet[] getOptimalBuildingBlockSubsets(){
        return this.optimalBuildingBlockSubsets;
    }

    public double getMaximalEntropy(){
        return this.entropy;
    }
}
