package combinatorial_molecule_library_design;

import java.util.Arrays;

public abstract class EntropyCalculator {

    protected double[][] bbMasses;
    protected double entropy;

    public EntropyCalculator(double[][] bbMasses){
        this.bbMasses = bbMasses;
        this.entropy = 0;
    }

    public abstract double computeEntropy();

    public double[][] getBuildingBlockMasses(){
        double[][] copyOfBbMasses = new double[this.bbMasses.length][];
        for(int i = 0; i < this.bbMasses.length; i++){
            copyOfBbMasses[i] = Arrays.copyOf(this.bbMasses[i], this.bbMasses[i].length);
        }
        return copyOfBbMasses;
    }
}
