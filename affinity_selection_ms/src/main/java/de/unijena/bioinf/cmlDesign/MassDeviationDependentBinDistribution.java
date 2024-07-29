package de.unijena.bioinf.cmlDesign;

import java.util.ArrayList;

public class MassDeviationDependentBinDistribution extends CMLDistribution{

    private double relDev;

    public MassDeviationDependentBinDistribution(double[][] bbMasses, double blowupFactor, double ppm) {
        super(bbMasses, blowupFactor);
        this.relDev = ppm * 1e-6;
        this.binEdges = this.computeBinEdges();
    }

    public MassDeviationDependentBinDistribution(int[][] bbMasses, double ppm){
        super(bbMasses);
        this.relDev = ppm * 1e-6;
        this.binEdges = this.computeBinEdges();
    }

    private int[] computeBinEdges(){
        // Initialisation:
        int minMoleculeMass = CMLUtils.getMinMoleculeMass(this.getBbMasses());
        int maxMoleculeMass = CMLUtils.getMaxMoleculeMass(this.getBbMasses());

        // Compute the bin edges in the integer range [minMoleculeMass, maxMoleculeMass]:
        double nextBinFactor = this.relDev < 1 ? (1 + this.relDev) / (1 - this.relDev) : (1 + 2 * this.relDev); // stability
        int previousBinEdge = minMoleculeMass;
        int nextBinEdge = (int) (previousBinEdge * nextBinFactor);
        if(maxMoleculeMass < nextBinEdge) return new int[]{minMoleculeMass, maxMoleculeMass};

        ArrayList<Integer> binEdges = new ArrayList<>();
        binEdges.add(minMoleculeMass);
        while(nextBinEdge <= maxMoleculeMass) {
            binEdges.add(nextBinEdge);
            previousBinEdge = nextBinEdge;
            nextBinEdge = (int) (previousBinEdge * nextBinFactor);
        }
        /* There are two cases:
         * - previousBinEdge = maxMoleculeMass:
         * ------> Finish!
         * - previousBinEdge < maxMoleculeMass
         * ------> The bin sizes have to be adjusted corresponding to the difference maxMoleculeMass - previousBinEdge
         */
        int numBins = binEdges.size() - 1;
        int diff = maxMoleculeMass - previousBinEdge;
        if(diff > 0){ // previousBinEdge < maxMoleculeMass and the bin widths have to be adjusted equally!
            int extensionForEachBin = diff / numBins;
            int rest = diff - numBins * extensionForEachBin;

            // Add extensionForEachBin to each bin:
            if(extensionForEachBin > 0) {
                for (int i = 1; i < binEdges.size(); i++) {
                    int modifiedBinEdge = binEdges.get(i) + i * extensionForEachBin;
                    binEdges.set(i, modifiedBinEdge);
                }
            }

            // For each bin with idx=binEdges.size()-rest,...,binEdges.size()-1, widen the bin by one integer mass:
            for(int i = 0; i < rest; i++){
                int idx = binEdges.size() - rest + i;
                int modifiedBinEdge = binEdges.get(idx) + i + 1;
                binEdges.set(idx, modifiedBinEdge);
            }

        }
        int[] binEdgesArray = new int[binEdges.size()];
        for(int i = 0; i < binEdges.size(); i++) binEdgesArray[i] = binEdges.get(i);
        return binEdgesArray;
    }

    public double getPpm(){
        return this.relDev * 1e6;
    }

    public double getRelDeviation(){
        return this.relDev;
    }
}
