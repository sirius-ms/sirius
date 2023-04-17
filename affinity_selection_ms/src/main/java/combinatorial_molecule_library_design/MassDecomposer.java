package combinatorial_molecule_library_design;

public class MassDecomposer {

    private final int[][] bbMasses;

    public MassDecomposer(double[][] bbMasses, double blowupFactor){
        // Transform the masses of the building blocks into integer masses using the blowup factor:
        this.bbMasses = new int[bbMasses.length][];

        for(int i = 0; i < bbMasses.length; i++) {
            this.bbMasses[i] = new int[bbMasses[i].length];
            for (int j = 0; j < bbMasses[i].length; j++) {
                this.bbMasses[i][j] = (int) (blowupFactor * bbMasses[i][j]);
            }
        }
    }

    public MassDecomposer(int[][] bbMasses){
        this.bbMasses = bbMasses;
    }

    public int numberOfMoleculesForIntegerMass(int mass){
        // Initialisation:
        int[][] numMols = new int[2][mass+1];
        numMols[0][0] = 1;  // only the empty string has mass 0

        // Loop:
        // For each i = 1,...,bbMasses.length and m = 0,...,mass
        // compute numMols[i][m] := number of strings s_1...s_i (first i building blocks) and mass m.
        int currentRow = 0;
        int previousRow = 1;
        int helperReferenceVariable = 0;
        for(int i = 1; i <= this.bbMasses.length; i++){
            helperReferenceVariable = currentRow;
            currentRow = previousRow;
            previousRow = helperReferenceVariable;

            for(int m = 0; m <= mass; m++){
                int sum = 0;
                for(int x : this.bbMasses[i-1]){
                    if(m - x >= 0) {
                        sum = sum + numMols[previousRow][m - x];
                    }
                }
                numMols[currentRow][m] = sum;
            }
        }
        return numMols[currentRow][mass];
    }


    public int numberOfMoleculesForInterval(int lowerBound, int upperBound){
        int numMolsInInterval = 0;
        for(int m = lowerBound; m <= upperBound; m++){
            numMolsInInterval = numMolsInInterval + this.numberOfMoleculesForIntegerMass(m);
        }
        return numMolsInInterval;
    }
}
