package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

/**
 * Created by ge28quv on 16/05/17.
 */
public class FragmentWithIndex implements Comparable<FragmentWithIndex> {
    public final MolecularFormula mf;
    public final short idx;
    public final double score;
    private final Ionization ionization;

    public FragmentWithIndex(MolecularFormula mf, Ionization ion, short idx, double score) {
        this.mf = mf;
        this.idx = idx;
        this.score = score;
        this.ionization = ion;
    }

    @Override
    public int compareTo(FragmentWithIndex o) {
        return mf.compareTo(o.mf);
    }

    public MolecularFormula getFormula() {
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
