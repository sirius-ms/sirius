package combinatorial_molecule_library_design;

public class CMLUtils {

    public static int[][] convertBBMassesToInteger(double[][] bbMasses, double blowupFactor){
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

    public static int getMinMoleculeMass(int[][] bbMasses){
        // It is assumed that the bbMasses are sorted in increasing order:
        int minMass = 0;
        for (int[] bbMass : bbMasses) {
            minMass = minMass + bbMass[0];
        }
        return minMass;
    }

    public static int getMaxMoleculeMass(int[][] bbMasses){
        // It is assumed that the bbMasses are sorted in increasing order:
        int maxMass = 0;
        for (int[] bbMass : bbMasses) {
            int maxBBMassIdx = bbMass.length - 1;
            maxMass = maxMass + bbMass[maxBBMassIdx];
        }
        return maxMass;
    }
}
