package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.EdgeFilter;
import de.unijena.bioinf.GibbsSampling.model.Graph;
import de.unijena.bioinf.jjobs.MasterJJob;

public class NoEdgeFilter extends AbstractEdgeFilter {
    public NoEdgeFilter() {
    }

    public void filterEdgesAndSetThreshold(Graph graph, int candidateIdx, double[] logEdgeScores) {
        graph.setEdgeThreshold(candidateIdx, 0.0D / 0.0);
        int peakIdx = graph.getPeakIdx(candidateIdx);

        for(int i = 0; i < logEdgeScores.length; ++i) {
            if(peakIdx != graph.getPeakIdx(i)) {
                double score = logEdgeScores[i];
                if(score != 0.0D && !Double.isInfinite(score)) {
                    graph.setLogWeight(candidateIdx, i, score);
                }
            }
        }

    }

    public int[][] postprocessCompleteGraph(Graph graph, MasterJJob masterJJob) {
        return this.makeWeightsSymmetricAndCreateConnectionsArray(graph);
    }

    public void setThreshold(double threshold) {
    }
}
