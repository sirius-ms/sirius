package de.unijena.bioinf.GibbsSampling.model;

/**
 * Created by ge28quv on 12/05/17.
 */
public class FragmentsAndLosses {
    private String[] fragments;
    private String[] losses;

    public FragmentsAndLosses(String[] fragments, String[] losses) {
        this.fragments = fragments;
        this.losses = losses;
    }

    public String[] getFragments() {
        return fragments;
    }

    public String[] getLosses() {
        return losses;
    }
}
