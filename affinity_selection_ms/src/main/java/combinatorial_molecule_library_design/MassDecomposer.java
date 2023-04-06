package combinatorial_molecule_library_design;

public class MassDecomposer {

    private int[][] bbMasses;
    private double blowupFactor;

    public MassDecomposer(double[][] bbMasses, double blowupFactor){
        // Transform the masses of the building blocks into integer masses using the blowup factor:
        this.blowupFactor = blowupFactor;
        this.bbMasses = new int[bbMasses.length][];

        for(int i = 0; i < bbMasses.length; i++) {
            this.bbMasses[i] = new int[bbMasses[i].length];
            for (int j = 0; j < bbMasses[i].length; j++) {
                this.bbMasses[i][j] = (int) (blowupFactor * bbMasses[i][j]);
            }
        }
    }

    private int numberOfMoleculesForIntegerMass(int mass){
        // Initialisation:
        int[][] numMols = new int[2][mass+1];
        numMols[0][0] = 1;  // only the empty string has mass 0

        // Loop:
        // For each n = 1,...,bbMasses.length and m = 0,...,mass
        // compute numMols[n mod 2,m] := number of strings with length n (first n building blocks) and mass m.
        for(int n = 1; n <= this.bbMasses.length; n++){
            int currentRow = n % 2;
            int previousRow = (n-1) % 2;

            for(int m = 0; m <= mass; m++){
                int sum = 0;
                for(int x : this.bbMasses[n-1]){
                    if(m - x >= 0) {
                        sum = sum + numMols[previousRow][m - x];
                    }
                }
                numMols[currentRow][m] = sum;
            }
        }

        return numMols[this.bbMasses.length % 2][mass];
    }

    public int numberOfMoleculesForInterval(double lowerBound, double upperBound){
        int transformedLowerBound = (int) (this.blowupFactor * lowerBound);
        int transformedUpperBound = (int) (this.blowupFactor * upperBound);
        int numMolsInTotal = 0;
        for(int m = transformedLowerBound; m <= transformedUpperBound; m++){
            numMolsInTotal = numMolsInTotal + this.numberOfMoleculesForIntegerMass(m);
        }
        return numMolsInTotal;
    }

    public static void main(String[] args){
        double[][] bbMasses = new double[][]{{2,3,7,10}, {2,3,7,10}, {2,3,7,10}, {2,3,7,10}};
        double blowupFactor = 1.0;
        MassDecomposer decomposer = new MassDecomposer(bbMasses, blowupFactor);
        System.out.println(decomposer.numberOfMoleculesForIntegerMass(10));
    }
}
