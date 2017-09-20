package de.unijena.bioinf.GibbsSampling.model;

import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class EdgeThresholdMinConnectionsFilter extends LocalEdgeFilter {
    private double basicThreshold;
    private int numberOfCandidatesWithMinConnCount;
    private int minimumConnectionCount;

    public EdgeThresholdMinConnectionsFilter(double basicThreshold, int numberOfCandidatesWithMinConnCount, int minimumConnectionCount) {
        super(Double.NaN);
        System.out.println("EdgeThresholdMinConnectionsFilter " + basicThreshold + " " + numberOfCandidatesWithMinConnCount + " " + minimumConnectionCount);
        if(minimumConnectionCount < 0) {
            throw new IllegalArgumentException("min connection count must be positive");
        } else if(numberOfCandidatesWithMinConnCount<0){
            throw new IllegalArgumentException("number of candidates with minimum number of connections must be >= 0 ");
        } else {
            this.basicThreshold = Math.log(basicThreshold);
            this.numberOfCandidatesWithMinConnCount = numberOfCandidatesWithMinConnCount;
            this.minimumConnectionCount = minimumConnectionCount;
        }
    }

    public void filterEdgesAndSetThreshold(Graph graph, int candidateIdx, double[] logEdgeScores) {
        ArrayList<WeightedEdge> weightedEdges = new ArrayList<>();
        int peakIdx = graph.getPeakIdx(candidateIdx);

        for(int i = 0; i < logEdgeScores.length; ++i) {
            if(peakIdx != graph.getPeakIdx(i)) {
                double score = logEdgeScores[i];
                weightedEdges.add(new WeightedEdge(candidateIdx, i, score));
            }
        }

        Collections.sort(weightedEdges);
//        System.out.println("weights " + weightedEdges.size() + " " + ((WeightedEdge)weightedEdges.get(0)).weight + " " + ((WeightedEdge)weightedEdges.get(weightedEdges.size() - 1)).weight);
        double currentThreshold;
        if(this.minimumConnectionCount >= weightedEdges.size()) {
            currentThreshold = weightedEdges.get(weightedEdges.size() - 1).weight;
        } else {
            currentThreshold = weightedEdges.get(this.minimumConnectionCount).weight;
        }

        if(Double.isInfinite(currentThreshold)) {
            for(int i = Math.min(this.minimumConnectionCount, weightedEdges.size()) - 1; i >= 0; --i) {
                double weightedEdge = weightedEdges.get(i).weight;
                if(isFinite(weightedEdge)) {
                    currentThreshold = weightedEdge;
                    break;
                }
            }

            if(Double.isInfinite(currentThreshold)) {
                currentThreshold = this.basicThreshold;
            }
        } else if(currentThreshold > this.basicThreshold) {
            currentThreshold = this.basicThreshold;
        }


        for (WeightedEdge weightedEdge : weightedEdges) {
            if(weightedEdge.weight <= currentThreshold) {
                break;
            }
            graph.setLogWeight(weightedEdge.index1, weightedEdge.index2, weightedEdge.weight - currentThreshold);
        }
        graph.setEdgeThreshold(candidateIdx, currentThreshold);
    }

    public void setThreshold(double threshold) {
        this.basicThreshold = threshold;
    }

    public int[][] postprocessCompleteGraph(Graph graph) {
        TIntArrayList[] connectionsList = new TIntArrayList[graph.getSize()];
        
        for(int i = 0; i < graph.getSize(); ++i) {
            connectionsList[i] = new TIntArrayList(100);
        }
        
        for(int i = 0; i < graph.numberOfCompounds(); ++i) {
            int left = graph.getPeakLeftBoundary(i);
            int right = graph.getPeakRightBoundary(i);
            double[] thresholds = new double[right - left + 1];

            int w2;
            for(w2 = left; w2 <= right; ++w2) {
                thresholds[w2 - left] = graph.getEdgeThreshold(w2);
            }

            Arrays.sort(thresholds);
//            w2 = (int)(this.numberOfCandidatesWithMinConnCount * (double)thresholds.length);
            w2 = Math.min(this.numberOfCandidatesWithMinConnCount, thresholds.length); //changed
            double t = w2 == 0?thresholds[thresholds.length - 1]:thresholds[thresholds.length - w2];
            if(t > this.basicThreshold) {
                t = this.basicThreshold;
            }

            for(int j = left; j <= right; ++j) {
                double current_t = graph.getEdgeThreshold(j);
                if(left == j) {
//                    System.out.println("before " + current_t + " " + t);
                }

                if(current_t != t) {
                    double diff = t - current_t;
                    int[] connections1 = graph.getLogWeightConnections(j);
//                    System.out.println("connection size " + connections1.length);
//                    if(connections1.length < 10) {
//                        System.out.println(((StandardCandidate)graph.getPossibleFormulas1D(j).getCandidate()).getFormula().formatByHill() + " is connected to " + Arrays.toString(connections1));
//                    }


                    for(int k = 0; k < connections1.length; ++k) {
                        int c = connections1[k];
                        double w = graph.getLogWeight(j, c);
                        graph.setLogWeight(j, c, Math.max(0.0D, w - diff));
                    }
                }
            }
        }

        for(int i = 0; i < graph.getSize(); ++i) {
            for(int j = i + 1; j < graph.getSize(); ++j) {
                double a = graph.getLogWeight(i, j);
                double b = graph.getLogWeight(j, i);
                double max;
                if(a < b) {
                    graph.setLogWeight(i, j, b);
                    max = b;
                } else if(b < a) {
                    graph.setLogWeight(j, i, a);
                    max = a;
                } else {
                    max = a;
                }

                if(max != 0.0D) {
                    connectionsList[i].add(j);
                    connectionsList[j].add(i);
                }
            }
        }

        int[][] connections = new int[graph.getSize()][];

        for(int i = 0; i < graph.getSize(); ++i) {
            connections[i] = connectionsList[i].toArray();
//            if(connections[i].length == 0) {
//                System.out.println("no connections at all for "+ ((StandardCandidate)graph.getPossibleFormulas1D(i).getCandidate()).getExperiment().getName() +" with " + ((StandardCandidate)graph.getPossibleFormulas1D(i).getCandidate()).getFormula().formatByHill());
//            }
        }

        return connections;
    }


    private boolean isFinite(double d) {
        return Math.abs(d) <= Double.MAX_VALUE;
    }
}
