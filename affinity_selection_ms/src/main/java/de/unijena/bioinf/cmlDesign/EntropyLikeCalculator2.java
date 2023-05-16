package de.unijena.bioinf.cmlDesign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EntropyLikeCalculator2 implements CMLEvaluator{

    private double relDev;
    private double blowupFactor;

    public EntropyLikeCalculator2(double ppm, double blowupFactor){
        this.relDev = ppm * 1e-6;
        this.blowupFactor = blowupFactor;
    }

    @Override
    public double evaluate(int[][] bbMasses) {
        File outputFile = new File("numCandidatesInWindow.txt");
        try(BufferedWriter fileWriter = Files.newBufferedWriter(outputFile.toPath())){

            // INITIALISATION:
            // We want to compute for each mass m = 0,..., maxMoleculeMass the number of molecules with mass m.
            int minMoleculeMass = CMLUtils.getMinMoleculeMass(bbMasses);
            int maxMoleculeMass = CMLUtils.getMaxMoleculeMass(bbMasses);
            int[] numMoleculesPerMass = CMLUtils.getNumMoleculesPerMass(bbMasses, maxMoleculeMass);

            /* LOOP:
             * The idea is to compute a mass-deviation-window and the number of candidates contained in this window for each
             * mass m with numMolecules[m] > 0. To avoid double counting of candidates,
             * we shift this window from minMoleculeMass to maxMoleculeMass.
             */

            // 1.) Initialise this window:
            int currentLowerBound = (int) (minMoleculeMass - this.relDev * minMoleculeMass);
            int currentUpperBound = (int) (minMoleculeMass + this.relDev * minMoleculeMass);
            if (currentUpperBound > maxMoleculeMass) currentUpperBound = maxMoleculeMass;

            int numCandidatesInWindow = 0;
            for (int m = minMoleculeMass; m <= currentUpperBound; m++)
                numCandidatesInWindow = numCandidatesInWindow + numMoleculesPerMass[m];

            fileWriter.write(minMoleculeMass+"\t"+numCandidatesInWindow);

            // 2.) Initialise the important sums for the score computation:
            double blowupFactorReciprocal = 1d / this.blowupFactor;
            double normalizedNumCandidatesInWindow2 = numCandidatesInWindow * blowupFactorReciprocal;
            double normalizedNumCandidatesInWindow3 = (numCandidatesInWindow * blowupFactorReciprocal) / minMoleculeMass;

            double logCandidatesSum1 = numCandidatesInWindow * Math.log(numCandidatesInWindow);
            double logCandidatesSum2 = normalizedNumCandidatesInWindow2 * Math.log(normalizedNumCandidatesInWindow2);
            double logCandidatesSum3 = normalizedNumCandidatesInWindow3 * Math.log(normalizedNumCandidatesInWindow3);

            double candidatesSum1 = numCandidatesInWindow;
            double candidatesSum2 = normalizedNumCandidatesInWindow2;
            double candidatesSum3 = normalizedNumCandidatesInWindow3;

            int newLowerBound, newUpperBound;
            for (int mass = minMoleculeMass + 1; mass <= maxMoleculeMass; mass++) {
                if (numMoleculesPerMass[mass] > 0) {
                    newLowerBound = (int) (mass - this.relDev * mass);
                    newUpperBound = (int) (mass + this.relDev * mass);
                    if (newUpperBound > maxMoleculeMass) newUpperBound = maxMoleculeMass;

                    if (newLowerBound <= currentUpperBound) {
                        for (int m = currentLowerBound; m < newLowerBound; m++)
                            numCandidatesInWindow -= numMoleculesPerMass[m];
                        for (int m = currentUpperBound + 1; m <= newUpperBound; m++)
                            numCandidatesInWindow += numMoleculesPerMass[m];
                    } else {
                        numCandidatesInWindow = 0;
                        for (int m = newLowerBound; m <= newUpperBound; m++)
                            numCandidatesInWindow += numMoleculesPerMass[m];
                    }

                    fileWriter.newLine();
                    fileWriter.write(mass+"\t"+numCandidatesInWindow);

                    normalizedNumCandidatesInWindow2 = numCandidatesInWindow * blowupFactorReciprocal;
                    normalizedNumCandidatesInWindow3 = (numCandidatesInWindow * blowupFactorReciprocal) / mass;

                    logCandidatesSum1 += numCandidatesInWindow * Math.log(numCandidatesInWindow);
                    logCandidatesSum2 += normalizedNumCandidatesInWindow2 * Math.log(normalizedNumCandidatesInWindow2);
                    logCandidatesSum3 += normalizedNumCandidatesInWindow3 * Math.log(normalizedNumCandidatesInWindow3);

                    candidatesSum1 += numCandidatesInWindow;
                    candidatesSum2 += normalizedNumCandidatesInWindow2;
                    candidatesSum3 += normalizedNumCandidatesInWindow3;

                    currentLowerBound = newLowerBound;
                    currentUpperBound = newUpperBound;
                }
            }

            int totalNumMolecules = CMLUtils.getTotalNumberOfMolecules(bbMasses);
            double totalNumMoleculesReciprocal = 1d / totalNumMolecules;

            double h1_1 = -totalNumMoleculesReciprocal * logCandidatesSum1 + Math.log(totalNumMolecules) * totalNumMoleculesReciprocal * candidatesSum1;
            double h1_2 = -totalNumMoleculesReciprocal * logCandidatesSum2 + Math.log(totalNumMolecules) * totalNumMoleculesReciprocal * candidatesSum2;
            double h1_3 = -totalNumMoleculesReciprocal * logCandidatesSum3 + Math.log(totalNumMolecules) * totalNumMoleculesReciprocal * candidatesSum3;

            double h2_1 = - logCandidatesSum1;
            double h2_2 = - logCandidatesSum2;
            double h2_3 = - logCandidatesSum3;

            System.out.println("H_1_1: "+h1_1);
            System.out.println("H_1_2: "+h1_2);
            System.out.println("H_1_3: "+h1_3+'\n');
            System.out.println("H_2_1: "+h2_1);
            System.out.println("H_2_2: "+h2_2);
            System.out.println("H_2_3: "+h2_3);

            return -totalNumMoleculesReciprocal * logCandidatesSum1 + Math.log(totalNumMolecules) * totalNumMoleculesReciprocal * candidatesSum1;
        }catch(IOException e){
            e.printStackTrace();
            return Double.NEGATIVE_INFINITY;
        }
    }
}