package de.unijena.bioinf.cmlDesign;

public class EquidistantBinDistribution extends CMLDistribution{

    private int binSize;

    public EquidistantBinDistribution(double[][] bbMasses, double blowupFactor, double binSize) {
        super(bbMasses, blowupFactor);
        this.binSize = (int) (blowupFactor * binSize);
        this.binEdges = this.computeBinEdges();
    }

    public EquidistantBinDistribution(int[][] bbMasses, int binSize){
        super(bbMasses);
        this.binSize = binSize;
        this.binEdges = this.computeBinEdges();
    }

    private int[] computeBinEdges(){
        // Initialisation:
        int minMoleculeMass = CMLUtils.getMinMoleculeMass(this.getBbMasses());
        int maxMoleculeMass = CMLUtils.getMaxMoleculeMass(this.getBbMasses());
        int lengthOfInterval = maxMoleculeMass - minMoleculeMass + 1;
        int numBins = lengthOfInterval / this.binSize;
        int rest = lengthOfInterval - numBins * this.binSize;

        // Computing the bin edges:
        // Reminder: we partition the interval in [],(],(],(]...,(] bins!
        int[] binEdges = new int[numBins + 1];
        binEdges[0] = minMoleculeMass;
        binEdges[1] = minMoleculeMass + this.binSize - 1;
        for(int i = 2; i <= numBins; i++){
            binEdges[i] = binEdges[i-1] + this.binSize;
        }

        // Divide the rest equally among all the bins:
        int extensionForEachBin = rest / numBins;
        rest = rest - numBins * extensionForEachBin;
        for(int i = 1; i <= numBins; i++){
            if(i <= rest) {
                binEdges[i] = binEdges[i] + i * extensionForEachBin + i;
            }else{
                binEdges[i] = binEdges[i] + i * extensionForEachBin + rest;
            }
        }
        return binEdges;
    }
    public int getBinSize(){
        return this.binSize;
    }
}
