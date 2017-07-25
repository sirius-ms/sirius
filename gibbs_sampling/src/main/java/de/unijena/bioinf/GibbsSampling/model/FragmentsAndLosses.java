package de.unijena.bioinf.GibbsSampling.model;

/**
 * Created by ge28quv on 12/05/17.
 */
public class FragmentsAndLosses {
    private final FragmentWithIndex[] fragments;
    private final FragmentWithIndex[] losses;

    public FragmentsAndLosses(FragmentWithIndex[] fragments, FragmentWithIndex[] losses) {
        this.fragments = fragments;
        this.losses = losses;
    }

    public FragmentWithIndex[] getFragments() {
        return fragments;
    }

    public FragmentWithIndex[] getLosses() {
        return losses;
    }

//    public short[] getFragIndices() {
//        return fragIndices;
//    }
//
//    public short[] getLossIndices() {
//        return lossIndices;
//    }
//
//    public double[] getFragmentScores() {
//        return fragmentScores;
//    }
//
//    public double[] getLossScores() {
//        return lossScores;
//    }
}
