package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.EdgeFilter;
import de.unijena.bioinf.GibbsSampling.model.Graph;
import de.unijena.bioinf.jjobs.MasterJJob;

public class EdgeThresholdFilter extends AbstractEdgeFilter {
    private double threshold;
    private double logThres;

    public EdgeThresholdFilter(double probabilityThreshold) {
        this.threshold = probabilityThreshold;
        this.logThres = Math.log(this.threshold);
    }

    public void filterEdgesAndSetThreshold(Graph graph, int candidateIdx, double[] logEdgeScores) {
        graph.setEdgeThreshold(candidateIdx, this.logThres);
        int peakIdx = graph.getPeakIdx(candidateIdx);

        for(int i = 0; i < logEdgeScores.length; ++i) {
            if(peakIdx != graph.getPeakIdx(i)) {
                double score = logEdgeScores[i];
                if(score < this.logThres) {
                    graph.setLogWeight(candidateIdx, i, this.logThres - score);
                }
            }
        }
    }

    public void setThreshold(double threshold) {
        this.logThres = threshold;
    }

    public int[][] postprocessCompleteGraph(Graph graph, MasterJJob masterJJob) {
        return this.makeWeightsSymmetricAndCreateConnectionsArray(graph);
    }

    public double getThreshold() {
        return this.threshold;
    }

    public double getLogThreshold() {
        return this.logThres;
    }
}
