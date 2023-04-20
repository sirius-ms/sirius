package combinatorial_molecule_library_design;

import static java.lang.Math.ceil;

public class EquidistantBinDistribution extends CMLDistribution{

    private double binSize;

    public EquidistantBinDistribution(double[][] bbMasses, double blowupFactor, double binSize) {
        super(bbMasses, blowupFactor);
    }

    private int[] computeBinEdges(){
        return new int[0];
    }
    public double getBinSize(){
        return this.binSize;
    }
}
