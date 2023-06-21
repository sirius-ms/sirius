package de.unijena.bioinf.cmlDesign;

// For all utility methods in this class, notice that bbMasses has to be sorted in increasing order
public class CMLUtils {

    public static int[][] convertBBMassesToInteger(double[][] bbMasses, double blowupFactor) {
        // Transform the masses of the building blocks into integer masses using the blowup factor:
        int[][] intBBMasses = new int[bbMasses.length][];
        for (int i = 0; i < bbMasses.length; i++) {
            intBBMasses[i] = new int[bbMasses[i].length];
            for (int j = 0; j < bbMasses[i].length; j++) {
                intBBMasses[i][j] = (int) (blowupFactor * bbMasses[i][j]);
            }
        }
        return intBBMasses;
    }

    public static double getAverageNumCandidatesPerMass(int[][] bbMasses, double relDev) {
        // 1.) Compute the maxMoleculeMass and for each m = 0,...,maxMoleculeMass the number of molecules with mass m:
        int minMoleculeMass = getMinMoleculeMass(bbMasses);
        int maxMoleculeMass = getMaxMoleculeMass(bbMasses);
        int[] numMolecules = getNumMoleculesPerMass(bbMasses, maxMoleculeMass);

        // 2.) Determine the number of masses m with numMolecules[m] > 0 and
        // the total sum of candidates for each observable mass:
        // 2.1: INITIALISATION:
        int currentLowerBound = (int) (minMoleculeMass - relDev * minMoleculeMass);
        int currentUpperBound = (int) (minMoleculeMass + relDev * minMoleculeMass);
        int currentNumCandidatesInWindow = 0;
        for(int m = currentLowerBound; m <=currentUpperBound; m++) currentNumCandidatesInWindow += numMolecules[m];

        // 2.2: LOOP
        int newLowerBound = 0, newUpperBound = 0;
        int sumCandidates = currentNumCandidatesInWindow;
        int numObservableMasses = 1;
        for(int mass = minMoleculeMass + 1; mass <= maxMoleculeMass; mass++){
            if(numMolecules[mass] > 0){
                newLowerBound = (int) (mass - relDev * mass);
                newUpperBound = (int) (mass + relDev * mass);
                if (newUpperBound > maxMoleculeMass) newUpperBound = maxMoleculeMass;

                for(int m = currentLowerBound; m < newLowerBound; m++) currentNumCandidatesInWindow -= numMolecules[m];
                for(int m = currentUpperBound + 1; m <= newUpperBound; m++) currentNumCandidatesInWindow += numMolecules[m];

                currentLowerBound = newLowerBound;
                currentUpperBound = newUpperBound;
                sumCandidates += currentNumCandidatesInWindow;
                numObservableMasses++;
            }
        }
        return (double) sumCandidates / numObservableMasses;
    }

    public static int[] getNumMoleculesPerMass(int[][] bbMasses) {
        return getNumMoleculesPerMass(bbMasses, getMaxMoleculeMass(bbMasses));
    }

    public static int[] getNumMoleculesPerMass(int[][] bbMasses, int mass) {
        MassDecomposer massDecomposer = new MassDecomposer(bbMasses);
        massDecomposer.numberOfMoleculesForIntegerMass(mass);
        return massDecomposer.getNumMolecules();
    }

    public static int getTotalNumberOfMolecules(int[][] bbMasses) {
        int totalNumMols = 1;
        for (int[] bbMass : bbMasses) totalNumMols = totalNumMols * bbMass.length;
        return totalNumMols;
    }

    public static int getMinMoleculeMass(int[][] bbMasses) {
        // It is assumed that the bbMasses are sorted in increasing order:
        int minMass = 0;
        for (int[] bbMass : bbMasses) {
            minMass = minMass + bbMass[0];
        }
        return minMass;
    }

    public static int getMaxMoleculeMass(int[][] bbMasses) {
        // It is assumed that the bbMasses are sorted in increasing order:
        int maxMass = 0;
        for (int[] bbMass : bbMasses) {
            int maxBBMassIdx = bbMass.length - 1;
            maxMass = maxMass + bbMass[maxBBMassIdx];
        }
        return maxMass;
    }
}
