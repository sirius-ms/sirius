package de.unijena.bioinf.cmlDesign;

public class EntropyCalculator implements CMLEvaluator {

    private CMLDistribution cmlDist;
    private Normalization normalization;

    public EntropyCalculator(CMLDistribution cmlDist, Normalization normalization){
        this.cmlDist = cmlDist;
        this.normalization = normalization;
    }

    public EntropyCalculator(CMLDistribution cmlDist){
        this(cmlDist, x -> x);
    }

    // Method that computes the entropy of a given combinatorial molecule library considering only the masses:
    @Override
    public double evaluate(int[][] bbMasses){
        int[] numMoleculesPerBin = this.cmlDist.computeNumMoleculesPerBin(bbMasses);
        int totalNumMolecules = this.sum(numMoleculesPerBin); // number of molecules in the interval defined by this.cmlDist!

        double entropy = 0;
        for(int binIdx = 0; binIdx < numMoleculesPerBin.length; binIdx++){
            double absFreq = numMoleculesPerBin[binIdx];
            double normalizedAbsFreq = this.normalization.normalize(absFreq);
            if(normalizedAbsFreq > 0) entropy = entropy + normalizedAbsFreq * Math.log(normalizedAbsFreq);
        }
        return totalNumMolecules > 0 ? - entropy / totalNumMolecules + Math.log(totalNumMolecules) : 0d;
    }

    private int sum(int[] a){
        int sum = 0;
        for(int x : a) sum = sum + x;
        return sum;
    }

    public CMLDistribution getCMLDistribution(){
        return this.cmlDist;
    }

}
