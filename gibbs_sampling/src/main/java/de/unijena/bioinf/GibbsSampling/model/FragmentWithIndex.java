package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;

/**
 * Created by ge28quv on 16/05/17.
 */
public class FragmentWithIndex implements Comparable<FragmentWithIndex> {
    public final String mf;
    public final short idx;
    public final double score;
    private final Ionization ionization;

    public FragmentWithIndex(String mf, Ionization ion, short idx, double score) {
        this.mf = mf;
        this.idx = idx;
        this.score = score;
        this.ionization = ion;
    }

    @Override
    public int compareTo(FragmentWithIndex o) {
        return mf.compareTo(o.mf);
    }

    public String getFormula() {
        return mf;
    }

    public short getIndex() {
        return idx;
    }

    public double getScore() {
        return score;
    }

    public Ionization getIonization() {
        return ionization;
    }
}
