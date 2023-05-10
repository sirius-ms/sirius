package de.unijena.bioinf.cmlDesign;

public class MassDecomposer {

    private final int[][] bbMasses;
    private int[][] numMols;
    private boolean isComputed;

    public MassDecomposer(double[][] bbMasses, double blowupFactor) {
        this.bbMasses = CMLUtils.convertBBMassesToInteger(bbMasses, blowupFactor);
        this.isComputed = false;
    }

    public MassDecomposer(int[][] bbMasses) {
        this.bbMasses = bbMasses;
        this.isComputed = false;
    }

    public int numberOfMoleculesForIntegerMass(int mass) {
        if (this.isComputed) {
            if (mass >= this.numMols[0].length) {
                // Create bigger matrix that can be used to compute this.numMols[this.bbMasses.length][mass]:
                int[][] copyNumMols = new int[this.bbMasses.length + 1][mass + 1];
                for (int i = 0; i < this.numMols.length; i++) {
                    for (int j = 0; j < this.numMols[0].length; j++) {
                        copyNumMols[i][j] = this.numMols[i][j];
                    }
                }

                // Determine the startMass for computing the rest of the matrix:
                int startMass = this.numMols[0].length;
                this.numMols = copyNumMols;
                this.computeMatrix(startMass);
            }
        } else {
            this.numMols = new int[this.bbMasses.length + 1][mass + 1];
            this.numMols[0][0] = 1;
            this.computeMatrix(0);
            this.isComputed = true;
        }
        return this.numMols[this.bbMasses.length][mass];
    }

    private void computeMatrix(int startMass) {
        // At this point, the matrix 'numMols' is already initialised and
        // in general, the entries for i = 0,...,this.bbMasses.length and m = 0,...,startMass-1 are already computed.
        for (int i = 1; i <= this.bbMasses.length; i++) {
            for (int m = startMass; m < this.numMols[0].length; m++) {
                // numMols[i,m] := number of strings s_1...s_i with mass m
                int sum = 0;
                for (int x : this.bbMasses[i - 1]) {
                    if (m - x >= 0) {
                        sum = sum + this.numMols[i - 1][m - x];
                    }
                }
                this.numMols[i][m] = sum;
            }
        }
    }

    public int numberOfMoleculesForInterval(int lowerBound, int upperBound) {
        int numMolInInterval = this.numberOfMoleculesForIntegerMass(upperBound);
        for(int m = lowerBound; m < upperBound; m++){
            numMolInInterval += this.numMols[this.bbMasses.length][m];
        }
        return numMolInInterval;
    }

    public int[][] getBbMasses() {
        return this.bbMasses;
    }

    public int[][] getNumMoleculesMatrix(){
        return this.numMols;
    }

    public int[] getNumMolecules(){
        return this.numMols[this.bbMasses.length];
    }

}