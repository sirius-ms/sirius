package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.Graph;
import gnu.trove.list.array.TIntArrayList;

public interface EdgeFilter {
    void filterEdgesAndSetThreshold(Graph var1, int var2, double[] var3);

    int[][] postprocessCompleteGraph(Graph var1);

    void setThreshold(double var1);

}
