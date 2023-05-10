package de.unijena.bioinf.cmlDesign;

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
        this.bbMasses = CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor);
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

        // The bin edges are separating the interval into these bins: [],(],(],...,(]
        for(int binIdx = this.numMoleculesPerBin.length-1; binIdx > 0; binIdx--){
            int lowerBound = this.binEdges[binIdx] + 1;
            int upperBound = this.binEdges[binIdx + 1];
            this.numMoleculesPerBin[binIdx] = massDecomposer.numberOfMoleculesForInterval(lowerBound, upperBound);
        }
        this.numMoleculesPerBin[0] = massDecomposer.numberOfMoleculesForInterval(this.binEdges[0], this.binEdges[1]);
        return this.numMoleculesPerBin;
    }

    public int[] computeNumMoleculesPerBin(double[][] bbMasses, double blowupFactor){
        return this.computeNumMoleculesPerBin(CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor));
    }

    public int[][] getBbMasses(){
        return this.bbMasses;
    }

    public int[] getNumMoleculesPerBin(){
        return this.numMoleculesPerBin;
    }

    public int[] getBinEdges(){
        return this.binEdges;
    }

}
