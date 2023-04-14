package combinatorial_molecule_library_design;

import java.util.ArrayList;

public class MassDeviationDependentBinDistribution extends CMLDistribution{

    private double ppm;

    public MassDeviationDependentBinDistribution(double[][] bbMasses, double blowupFactor, double ppm) {
        super(bbMasses, blowupFactor);
        this.ppm = ppm;

        // Case: minMoleculeMass == maxMoleculeMass
        double minMoleculeMass = this.getMinMoleculeMass();
        double maxMoleculeMass = this.getMaxMoleculeMass();
        if(minMoleculeMass == maxMoleculeMass){
            this.binEdges = new double[]{minMoleculeMass, maxMoleculeMass};
        }else{
            double relDev = ppm / Math.pow(10, 6);
            ArrayList<Double> binEdges = new ArrayList<>();
            binEdges.add(minMoleculeMass);

            double previousBinEdge = minMoleculeMass;
            double nextBinEdge = (previousBinEdge * (1 + relDev)) / (1 - relDev);
            while(nextBinEdge <= maxMoleculeMass){
                binEdges.add(nextBinEdge);
                previousBinEdge = nextBinEdge;
                nextBinEdge = (previousBinEdge * (1 + relDev)) / (1 - relDev);
            }

            // Now, we know: previousBinEdge <= maxMoleculeMass and nextBinEdge > maxMoleculeMass
            // We want to equally distribute the space between previousBinEdge to maxMoleculeMass over the other bins.
            int numBins = binEdges.size() - 1;
            double additionalSpacePerBin = (maxMoleculeMass - previousBinEdge) / numBins;

            this.binEdges = new double[binEdges.size()];
            this.binEdges[0] = binEdges.get(0);
            this.binEdges[numBins] = maxMoleculeMass;
            for(int i = 1; i < numBins; i++){
                this.binEdges[i] = binEdges.get(i) + i * additionalSpacePerBin;
            }
        }
    }

    public double getPpm(){
        return this.ppm;
    }
}
