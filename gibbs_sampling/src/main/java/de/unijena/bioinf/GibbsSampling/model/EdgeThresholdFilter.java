package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.EdgeFilter;
import de.unijena.bioinf.GibbsSampling.model.Graph;

public class EdgeThresholdFilter extends AbstractEdgeFilter {
    private static final boolean DEBUG = false;
    private double threshold;
    private double logThres;

    public EdgeThresholdFilter(double probabilityThreshold) {
        this.threshold = probabilityThreshold;
        this.logThres = Math.log(this.threshold);
    }

    public void filterEdgesAndSetThreshold(Graph graph, int candidateIdx, double[] logEdgeScores) {
        graph.setEdgeThreshold(candidateIdx, this.logThres);
        int peakIdx = graph.getPeakIdx(candidateIdx);

        int counter = 0;
        for(int i = 0; i < logEdgeScores.length; ++i) {
            if(peakIdx != graph.getPeakIdx(i)) {
                double score = logEdgeScores[i];
                if(score < this.logThres) {
                    ++counter;
                    graph.setLogWeight(candidateIdx, i, this.logThres - score);
                }
            }
        }
        if (DEBUG) System.out.println("added edges: "+counter);

    }

    public void setThreshold(double threshold) {
        this.logThres = threshold;
    }

    public int[][] postprocessCompleteGraph(Graph graph) {
        return this.makeWeightsSymmetricAndCreateConnectionsArray(graph);
    }

    public double getThreshold() {
        return this.threshold;
    }

    public double getLogThreshold() {
        return this.logThres;
    }
}
