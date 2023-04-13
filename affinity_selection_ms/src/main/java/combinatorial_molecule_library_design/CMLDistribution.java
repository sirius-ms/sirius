package combinatorial_molecule_library_design;

/**
 * An object of this class CMLDistribution enables the computation of the distribution of the molecular masses
 * over the mass range for each combinatorial molecule library.<br>
 * This distribution is computed by splitting the mass range of interest into several bins and
 * computing the number of molecules for each bin.
 * Because the binning procedure can differ (e.g. equidistant bins, bins of different sizes),
 * this class declared as abstract.
 */
public abstract class CMLDistribution {

    private final double[][] bbMasses;
    private final double blowupFactor;
    protected double[] binEdges;
    private int[] numMoleculesPerBin;

    public CMLDistribution(double[][] bbMasses, double blowupFactor){
        this.bbMasses = bbMasses;
        this.blowupFactor = blowupFactor;
    }

    public int[] computeNumMoleculesPerBin(){
        MassDecomposer massDecomposer = new MassDecomposer(this.bbMasses, this.blowupFactor);
        this.numMoleculesPerBin = new int[this.binEdges.length - 1];
        for(int binIdx = 0; binIdx < this.numMoleculesPerBin.length - 1; binIdx++){
            int transformedLowerBound = (int) (this.blowupFactor * this.binEdges[binIdx]);
            int transformedUpperBound = (int) (this.blowupFactor * this.binEdges[binIdx + 1]);
            this.numMoleculesPerBin[binIdx] = massDecomposer.numberOfMoleculesForInterval(transformedLowerBound, transformedUpperBound - 1);
        }
        this.numMoleculesPerBin[this.numMoleculesPerBin.length - 1] = massDecomposer.
                numberOfMoleculesForInterval((int) (this.blowupFactor * this.binEdges[this.binEdges.length - 2]), (int) (this.blowupFactor * this.binEdges[this.binEdges.length - 1]));
        return this.numMoleculesPerBin;
    }

    public double getMinMoleculeMass(){
        // It is assumed that the bbMasses are sorted in increasing order:
        double minMass = 0;
        for(int idx = 0; idx < this.bbMasses.length; idx++) {
            minMass = minMass + this.bbMasses[idx][0];
        }
        return minMass;
    }

    public double getMaxMoleculeMass(){
        // It is assumed that the bbMasses are sorted in increasing order:
        double maxMass = 0;
        for(int idx = 0; idx < this.bbMasses.length; idx++){
            int maxBBMassIdx = this.bbMasses[idx].length - 1;
            maxMass = maxMass + this.bbMasses[idx][maxBBMassIdx];
        }
        return maxMass;
    }

    public double[][] getBbMasses(){
        return this.bbMasses;
    }

    public double getBlowupFactor(){
        return this.blowupFactor;
    }

    public double[] getBinEdges(){
        return this.binEdges;
    }

    public int[] getNumMoleculesPerBin(){
        return this.numMoleculesPerBin;
    }

}
