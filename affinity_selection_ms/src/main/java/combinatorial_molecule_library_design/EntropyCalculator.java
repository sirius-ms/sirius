package combinatorial_molecule_library_design;

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
    // todo: renew normalization if possible
    @Override
    public double evaluate(int[][] bbMasses){
        int[] numMoleculesPerBin = this.cmlDist.computeNumMoleculesPerBin(bbMasses);
        int totalNumMolecules = this.sum(numMoleculesPerBin);

        double divisionByTotalNumMols = 1d / totalNumMolecules;
        double entropy = 0;
        for(int binIdx = 0; binIdx < numMoleculesPerBin.length; binIdx++){
            double relFreq = numMoleculesPerBin[binIdx] * divisionByTotalNumMols;
            double normalizedRelFreq = this.normalization.normalize(relFreq);
            if(normalizedRelFreq > 0) entropy = entropy + normalizedRelFreq * Math.log(normalizedRelFreq);
        }
        return -entropy;
    }

    private int sum(int[] a){
        int sum = 0;
        for(int x : a) sum = sum + x;
        return sum;
    }

}
