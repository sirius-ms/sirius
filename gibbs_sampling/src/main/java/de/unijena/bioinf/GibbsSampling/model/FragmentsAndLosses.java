package de.unijena.bioinf.GibbsSampling.model;

/**
 * Created by ge28quv on 12/05/17.
 */
public class FragmentsAndLosses {
    private String[] fragments;
    private String[] losses;

    private short[] fragIndices;
    private short[] lossIndices;

    public FragmentsAndLosses(String[] fragments, short[] fragIndices, String[] losses, short[] lossIndices) {
        this.fragments = fragments;
        this.losses = losses;
        this.fragIndices = fragIndices;
        this.lossIndices = lossIndices;
    }

    public String[] getFragments() {
        return fragments;
    }

    public String[] getLosses() {
        return losses;
    }

    public short[] getFragIndices() {
        return fragIndices;
    }

    public short[] getLossIndices() {
        return lossIndices;
    }
}
