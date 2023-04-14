package combinatorial_molecule_library_design;

import static java.lang.Math.ceil;

public class EquidistantBinDistribution extends CMLDistribution{

    private double binSize;

    public EquidistantBinDistribution(double[][] bbMasses, double blowupFactor, double binSize) {
        super(bbMasses, blowupFactor);

        // Case: minMoleculeMass == maxMoleculeMass =: x
        // --> all molecules in the library have the same mass and fall into one bin [x, x]
        double lengthOfInterval = this.getMaxMoleculeMass() - this.getMinMoleculeMass();
        if(lengthOfInterval == 0){
            this.binEdges = new double[]{this.getMinMoleculeMass(), this.getMaxMoleculeMass()};
            this.binSize = 0;
        }else{
            // We have to adjust the binSize in order to get bins of equal width:
            int numBins = (int) ceil(lengthOfInterval / binSize);
            this.binSize = lengthOfInterval / numBins;

            // Compute the bin edges:
            this.binEdges = new double[numBins + 1];
            this.binEdges[0] = this.getMinMoleculeMass();
            this.binEdges[numBins] = this.getMaxMoleculeMass();
            for(int i = 1; i < numBins; i++){
                this.binEdges[i] = this.binEdges[i-1] + this.binSize;
            }
        }
    }

    public double getBinSize(){
        return this.binSize;
    }
}
