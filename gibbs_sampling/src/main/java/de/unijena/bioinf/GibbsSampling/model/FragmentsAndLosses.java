package de.unijena.bioinf.GibbsSampling.model;

import java.util.Arrays;

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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FragmentsAndLosses that = (FragmentsAndLosses) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(fragments, that.fragments)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(losses, that.losses);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(fragments);
        result = 31 * result + Arrays.hashCode(losses);
        return result;
    }
}
