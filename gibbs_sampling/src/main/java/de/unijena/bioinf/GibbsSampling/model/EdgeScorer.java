//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.jjobs.BasicJJob;

public interface EdgeScorer<C extends Candidate<?>> {
    void setThreshold(double threshold);

    double getThreshold();

    void prepare(C[][] var1);

    double score(C var1, C var2);

    double scoreWithoutThreshold(C var1, C var2);

    void clean();

    double[] normalization(C[][] var1, double minimum_number_matched_peaks_losses);

    public BasicJJob<Object> getPrepareJob(C[][] var1);
}
