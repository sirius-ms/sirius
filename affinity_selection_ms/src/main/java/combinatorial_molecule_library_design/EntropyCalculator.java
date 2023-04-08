package combinatorial_molecule_library_design;

import java.util.Arrays;

public abstract class EntropyCalculator {

    protected final double[][] bbMasses;
    protected double entropy;

    public EntropyCalculator(double[][] bbMasses){
        this.bbMasses = bbMasses;
        this.entropy = 0;
    }

    public abstract double computeEntropy();

    public double[][] getBuildingBlockMasses(){
        return this.bbMasses;
    }
}
