package de.unijena.bioinf.GibbsSampling.model;

/**
 * Created by ge28quv on 16/05/17.
 */
public class FragmentWithIndex implements Comparable<FragmentWithIndex> {
    public final String mf;
    public final short idx;

    public FragmentWithIndex(String mf, short idx) {
        this.mf = mf;
        this.idx = idx;
    }

    @Override
    public int compareTo(FragmentWithIndex o) {
        return mf.compareTo(o.mf);
    }
}
