//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.MFCandidate;

public interface EdgeScorer {
    void prepare(MFCandidate[][] var1);

    double score(MFCandidate var1, MFCandidate var2);

    void clean();

    double[] normalization(MFCandidate[][] var1);
}
