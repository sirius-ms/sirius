package de.unijena.bioinf.GibbsSampling.model;

public interface NodeScorer<C extends Candidate<?>> {
    void score(C[] var1);
}
