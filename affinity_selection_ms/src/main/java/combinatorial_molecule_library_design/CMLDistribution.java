package combinatorial_molecule_library_design;

/**
 * An object of this class CMLDistribution enables the computation of the distribution of the molecular masses
 * over the mass range for each combinatorial molecule library.
 * This distribution is computed by splitting the mass range of interest into several bins and
 * computing the number of molecules for each bin using an object of class {@link MassDecomposer}.<br>
 * Because the binning procedure can differ (e.g. equidistant bins, bins of different sizes),
 * this class is declared as abstract.
 */
public abstract class CMLDistribution {

    private final int[][] bbMasses;
    protected int[] binEdges;
    protected int[] numMoleculesPerBin;

    public CMLDistribution(double[][] bbMasses, double blowupFactor){
        this.bbMasses = MassDecomposer.convertBBMassesToInteger(bbMasses, blowupFactor);
    }

    public CMLDistribution(int[][] bbMasses){
        this.bbMasses = bbMasses;
    }

    public int[] computeNumMoleculesPerBin(){
        return this.computeNumMoleculesPerBin(this.bbMasses);
    }

    public int[] computeNumMoleculesPerBin(int[][] bbMasses){
        MassDecomposer massDecomposer = new MassDecomposer(bbMasses);
        this.numMoleculesPerBin = new int[this.binEdges.length - 1];

        for(int binIdx = this.numMoleculesPerBin.length-1; binIdx > 0; binIdx--){
            int lowerBound = this.binEdges[binIdx] + 1;
            int upperBound = this.binEdges[binIdx + 1];
            this.numMoleculesPerBin[binIdx] = massDecomposer.numberOfMoleculesForInterval(lowerBound, upperBound);
        }
        this.numMoleculesPerBin[0] = massDecomposer.numberOfMoleculesForInterval(this.binEdges[0], this.binEdges[1]);
        return this.numMoleculesPerBin;
    }

    public int[] computeNumMoleculesPerBin(double[][] bbMasses, double blowupFactor){
        return this.computeNumMoleculesPerBin(MassDecomposer.convertBBMassesToInteger(bbMasses, blowupFactor));
    }

    public int getMinMoleculeMass(){
        // It is assumed that the bbMasses are sorted in increasing order:
        int minMass = 0;
        for(int idx = 0; idx < this.bbMasses.length; idx++) {
            minMass = minMass + this.bbMasses[idx][0];
        }
        return minMass;
    }

    public int getMaxMoleculeMass(){
        // It is assumed that the bbMasses are sorted in increasing order:
        int maxMass = 0;
        for(int idx = 0; idx < this.bbMasses.length; idx++){
            int maxBBMassIdx = this.bbMasses[idx].length - 1;
            maxMass = maxMass + this.bbMasses[idx][maxBBMassIdx];
        }
        return maxMass;
    }

    public int[][] getBbMasses(){
        return this.bbMasses;
    }

    public int[] getNumMoleculesPerBin(){
        return this.numMoleculesPerBin;
    }

}
