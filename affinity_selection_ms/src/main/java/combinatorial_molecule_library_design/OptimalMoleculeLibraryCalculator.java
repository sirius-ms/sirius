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
        for(int i = 0; i < this.bbMasses.length; i++){
            currentBuildingBlockSubsets[i] = new BitSet(this.bbMasses[i].length);
            currentBuildingBlockSubsets[i].set(0);
        }

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


    public static void main(String[] args){
        double[][] bbMasses = new double[][]{{87.0320284,97.052763844,113.084063972,137.058911844,147.068413908,163.063328528,186.07931294},{57.021463716,71.03711378,97.052763844,99.068413908,101.047678464,128.058577496,129.042593084}, {96.057514876,122.016792936,152.998141428,164.047344116,166.037842052}};
        for(double[] bbMassArray : bbMasses){
            for(double x : bbMassArray){
                System.out.print(x+" ");
            }
            System.out.println();
        }

        OptimalMoleculeLibraryCalculator optimalMoleculeLibraryCalculator = new OptimalMoleculeLibraryCalculator(bbMasses, 1000, 0.001);
        optimalMoleculeLibraryCalculator.computeOptimalBuildingBlockSubsets();
        for(BitSet bitset : optimalMoleculeLibraryCalculator.getOptimalBuildingBlockSubsets()){
            System.out.println(bitset);
        }
    }
}
