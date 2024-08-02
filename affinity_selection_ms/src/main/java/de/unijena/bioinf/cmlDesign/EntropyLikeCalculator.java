package de.unijena.bioinf.cmlDesign;

public class EntropyLikeCalculator implements CMLEvaluator{

    private double relDev;
    private double blowupFactor;

    public EntropyLikeCalculator(double ppm, double blowupFactor){
        this.relDev = ppm * 1e-6;
        this.blowupFactor = blowupFactor;
    }

    @Override
    public double evaluate(int[][] bbMasses) {
        // INITIALISATION:
        // We want to compute for each mass m = 0,..., maxMoleculeMass the number of molecules with mass m.
        int minMoleculeMass = CMLUtils.getMinMoleculeMass(bbMasses);
        int maxMoleculeMass = CMLUtils.getMaxMoleculeMass(bbMasses);
        int[] numMoleculesPerMass = CMLUtils.getNumMoleculesPerMass(bbMasses, maxMoleculeMass);

        /* LOOP:
         * The idea is to compute a mass-deviation-window and the number of candidates contained in this window for each
         * mass m in [minMoleculeMass, maxMoleculeMass]. To avoid double counting of candidates,
         * we shift this window from minMoleculeMass to maxMoleculeMass.
         */

        // 1.) Initialise this window:
        int currentLowerBound = (int) (minMoleculeMass - this.relDev * minMoleculeMass);
        int currentUpperBound = (int) (minMoleculeMass + this.relDev * minMoleculeMass);
        if(currentUpperBound > maxMoleculeMass) currentUpperBound = maxMoleculeMass;

        int numCandidatesInWindow = 0;
        for(int m = minMoleculeMass; m <= currentUpperBound; m++) numCandidatesInWindow = numCandidatesInWindow + numMoleculesPerMass[m];

        // 2.) Initialise the important sums for the score computation:
        double totalNumMolecules = CMLUtils.getTotalNumberOfMolecules(bbMasses);
        double totalNumMoleculesReciprocal = 1d / totalNumMolecules;

        double relativeNumCandidatesInWindow = numCandidatesInWindow * totalNumMoleculesReciprocal;
        double entropy = relativeNumCandidatesInWindow * Math.log(relativeNumCandidatesInWindow);

        int newLowerBound, newUpperBound;
        for(int mass = minMoleculeMass + 1; mass <= maxMoleculeMass; mass++){
            newLowerBound = (int) (mass - this.relDev * mass);
            newUpperBound = (int) (mass + this.relDev * mass);
            if(newUpperBound > maxMoleculeMass) newUpperBound = maxMoleculeMass;

            if(newLowerBound <= currentUpperBound){
                for(int m = currentLowerBound; m < newLowerBound; m++) numCandidatesInWindow -= numMoleculesPerMass[m];
                for(int m = currentUpperBound + 1; m <= newUpperBound; m++) numCandidatesInWindow += numMoleculesPerMass[m];
            }else{
                numCandidatesInWindow = 0;
                for(int m = newLowerBound; m <= newUpperBound; m++) numCandidatesInWindow += numMoleculesPerMass[m];
            }

            relativeNumCandidatesInWindow = numCandidatesInWindow * totalNumMoleculesReciprocal;
            if(relativeNumCandidatesInWindow > 0) entropy += relativeNumCandidatesInWindow * Math.log(relativeNumCandidatesInWindow);

            currentLowerBound = newLowerBound;
            currentUpperBound = newUpperBound;
        }

        return -entropy / this.blowupFactor;
    }
}
