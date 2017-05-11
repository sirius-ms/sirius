package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.GibbsSampling.model.Graph;
import de.unijena.bioinf.GibbsSampling.model.LocalEdgeFilter;
import de.unijena.bioinf.GibbsSampling.model.MFCandidate;
import de.unijena.bioinf.GibbsSampling.model.LocalEdgeFilter.WeightedEdge;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class EdgeThresholdMinConnectionsFilter extends LocalEdgeFilter {
    private double basicThreshold;
    private double ratioOfCandidatesWithMinConnCount;
    private int minimumConnectionCount;

    public EdgeThresholdMinConnectionsFilter(double basicThreshold, double ratioOfCandidatesWithMinConnCount, int minimumConnectionCount) {
        super(0.0D / 0.0);
        System.out.println("EdgeThresholdMinConnectionsFilter " + basicThreshold + " " + ratioOfCandidatesWithMinConnCount + " " + minimumConnectionCount);
        if(minimumConnectionCount < 0) {
            throw new IllegalArgumentException("min connection count must be positive");
        } else {
            this.basicThreshold = Math.log(basicThreshold);
            this.ratioOfCandidatesWithMinConnCount = ratioOfCandidatesWithMinConnCount;
            this.minimumConnectionCount = minimumConnectionCount;
        }
    }

    public void filterEdgesAndSetThreshold(Graph graph, int candidateIdx, double[] logEdgeScores) {
        ArrayList weightedEdges = new ArrayList();
        int peakIdx = graph.getPeakIdx(candidateIdx);

        for(int threshold = 0; threshold < logEdgeScores.length; ++threshold) {
            if(peakIdx != graph.getPeakIdx(threshold)) {
                double score = logEdgeScores[threshold];
                weightedEdges.add(new WeightedEdge(candidateIdx, threshold, score));
            }
        }

        Collections.sort(weightedEdges);
        System.out.println("weights " + weightedEdges.size() + " " + ((WeightedEdge)weightedEdges.get(0)).weight + " " + ((WeightedEdge)weightedEdges.get(weightedEdges.size() - 1)).weight);
        double var11;
        if(this.minimumConnectionCount >= weightedEdges.size()) {
            var11 = ((WeightedEdge)weightedEdges.get(weightedEdges.size() - 1)).weight;
        } else {
            var11 = ((WeightedEdge)weightedEdges.get(this.minimumConnectionCount)).weight;
        }

        if(Double.isInfinite(var11)) {
            for(int i = Math.min(this.minimumConnectionCount, weightedEdges.size()) - 1; i >= 0; --i) {
                double weightedEdge = ((WeightedEdge)weightedEdges.get(i)).weight;
                if(isFinite(weightedEdge)) {
                    var11 = weightedEdge;
                    break;
                }
            }

            if(Double.isInfinite(var11)) {
                var11 = this.basicThreshold;
            }
        } else if(var11 > this.basicThreshold) {
            var11 = this.basicThreshold;
        }

        System.out.println("thres2 " + var11 + " " + this.basicThreshold);
        Iterator var12 = weightedEdges.iterator();

        while(var12.hasNext()) {
            WeightedEdge var13 = (WeightedEdge)var12.next();
            if(var13.weight <= var11) {
                break;
            }

            graph.setLogWeight(var13.index1, var13.index2, var13.weight - var11);
        }

        graph.setEdgeThreshold(candidateIdx, var11);
    }

    public void setThreshold(double threshold) {
        this.basicThreshold = threshold;
    }

    public int[][] postprocessCompleteGraph(Graph graph) {
        TIntArrayList[] connectionsList = new TIntArrayList[graph.getSize()];

        int connections;
        for(connections = 0; connections < graph.getSize(); ++connections) {
            connectionsList[connections] = new TIntArrayList(100);
        }

        int i;
        for(connections = 0; connections < graph.numberOfCompounds(); ++connections) {
            i = graph.getPeakLeftBoundary(connections);
            int w1 = graph.getPeakRightBoundary(connections);
            System.out.println("boundaries " + i + " " + w1);
            double[] thresholds = new double[w1 - i + 1];

            int w2;
            for(w2 = i; w2 <= w1; ++w2) {
                thresholds[w2 - i] = graph.getEdgeThreshold(w2);
            }

            Arrays.sort(thresholds);
            w2 = (int)(this.ratioOfCandidatesWithMinConnCount * (double)thresholds.length);
            double t = w2 == 0?thresholds[thresholds.length - 1]:thresholds[thresholds.length - w2];
            if(t > this.basicThreshold) {
                t = this.basicThreshold;
            }

            for(int j = i; j <= w1; ++j) {
                double current_t = graph.getEdgeThreshold(j);
                if(i == j) {
                    System.out.println("before " + current_t + " " + t);
                }

                if(current_t != t) {
                    double diff = t - current_t;
                    int[] connections1 = graph.getLogWeightConnections(j);
                    System.out.println("connection size " + connections1.length);
                    if(connections1.length < 10) {
                        System.out.println(((MFCandidate)graph.getPossibleFormulas1D(j).getCandidate()).getFormula().formatByHill() + " is connected to " + Arrays.toString(connections1));
                    }

                    int[] var16 = connections1;
                    int var17 = connections1.length;

                    for(int var18 = 0; var18 < var17; ++var18) {
                        int c = var16[var18];
                        double w = graph.getLogWeight(j, c);
                        graph.setLogWeight(j, c, Math.max(0.0D, w - diff));
                    }
                }
            }
        }

        for(connections = 0; connections < graph.getSize(); ++connections) {
            for(i = connections + 1; i < graph.getSize(); ++i) {
                double var23 = graph.getLogWeight(connections, i);
                double var24 = graph.getLogWeight(i, connections);
                double max;
                if(var23 < var24) {
                    graph.setLogWeight(connections, i, var24);
                    max = var24;
                } else if(var24 < var23) {
                    graph.setLogWeight(i, connections, var23);
                    max = var23;
                } else {
                    max = var23;
                }

                if(max != 0.0D) {
                    connectionsList[connections].add(i);
                    connectionsList[i].add(connections);
                }
            }
        }

        int[][] var22 = new int[graph.getSize()][];

        for(i = 0; i < var22.length; ++i) {
            var22[i] = connectionsList[i].toArray();
            if(var22[i].length == 0) {
                System.out.println("no connections at all for " + ((MFCandidate)graph.getPossibleFormulas1D(i).getCandidate()).getFormula().formatByHill());
            }
        }

        return var22;
    }


    private boolean isFinite(double d) {
        return Math.abs(d) <= Double.MAX_VALUE;
    }
}
