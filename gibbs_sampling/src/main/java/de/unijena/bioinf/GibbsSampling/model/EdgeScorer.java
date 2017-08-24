//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.unijena.bioinf.GibbsSampling.model;

public interface EdgeScorer<C extends Candidate<?>> {
    void setThreshold(double threshold);

    void prepare(C[][] var1);

    double score(C var1, C var2);

    double scoreWithoutThreshold(C var1, C var2);

    void clean();

    double[] normalization(C[][] var1);
}
