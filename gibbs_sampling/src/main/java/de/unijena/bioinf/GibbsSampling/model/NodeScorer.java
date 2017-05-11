package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.MFCandidate;

public interface NodeScorer {
    void score(MFCandidate[][] var1);
}
