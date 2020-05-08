package de.unijena.bioinf.retention;

import de.unijena.bioinf.fingerid.fingerprints.ShortestPathFingerprinter;

import java.util.Set;

public class ShortestPathKernel implements MoleculeKernel<Set<String>> {


    @Override
    public Set<String> prepare(PredictableCompound compound) {
        return new ShortestPathFingerprinter.ShortestPathWalker(compound.getMolecule()).paths();
    }

    @Override
    public double compute(PredictableCompound left, PredictableCompound right, Set<String> preparedLeft, Set<String> preparedRight) {
        int intersection = 0;
        for (String s : preparedLeft) {
            if (preparedRight.contains(s))
                ++intersection;
        }
        int union = preparedLeft.size()+preparedRight.size()-intersection;
        return ((double)intersection)/union;
    }

}
