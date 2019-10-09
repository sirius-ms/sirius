package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.Graph;
import de.unijena.bioinf.jjobs.MasterJJob;
import gnu.trove.list.array.TIntArrayList;

import java.util.concurrent.ExecutionException;

public interface EdgeFilter {
    void filterEdgesAndSetThreshold(Graph var1, int var2, double[] var3);

    int[][] postprocessCompleteGraph(Graph var1, MasterJJob masterJJob) throws ExecutionException;

    void setThreshold(double var1);

}
