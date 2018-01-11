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
                //CHANGED just add edge if <0 (at least some matching fragments)
                if (score>=0) continue;

                //edges assigned in this 'reverse direction' (candidateIdx->i) to retrieve all of them later on
                //after that the graph is made symmetric anyways.
                weightedEdges.add(new WeightedEdge(candidateIdx, i, score));
            }
        }

        //negative weights are good (p-values)
        //take at least the top x edges. maybe more if several are better than basic thres.
        Collections.sort(weightedEdges);
        double currentThreshold;
//        if (weightedEdges.size()==0){
//            currentThreshold = basicThreshold;
//        } else if(this.minimumConnectionCount >= weightedEdges.size()) {
//            currentThreshold = weightedEdges.get(weightedEdges.size() - 1).weight;//todo add pseudo???
//        } else {
//            //get next best weight, so that minimumConnectionCount-1 edge gets positive weight
//            //if no bigger weight exists it is set to weight of minimumConnectionCount-1 edge
//            //using 0 probably makes the score likely to 'strong'
//            //todo test 0?
//            double thresholdScore = weightedEdges.get(minimumConnectionCount-1).weight;
//            currentThreshold = thresholdScore;
//            for (int i = minimumConnectionCount; i < weightedEdges.size(); i++) {
//                double weight = weightedEdges.get(i).weight;
//                if (weight>currentThreshold){
//                    currentThreshold = weight;
//                    break;
//                }
//
//            }
////            currentThreshold = weightedEdges.get(this.minimumConnectionCount).weight;
//        }

        //changed use 0 as possible threshold
        if (weightedEdges.size()==0){
            currentThreshold = 0;
        } else if(this.minimumConnectionCount >= weightedEdges.size()) {
            currentThreshold = 0;//todo add pseudo???
        } else {
            //get next best weight, so that minimumConnectionCount-1 edge gets positive weight
            //if no bigger weight exists it is set to weight of minimumConnectionCount-1 edge
            //using 0 probably makes the score likely to 'strong'
            //todo test 0?
            double thresholdScore = weightedEdges.get(minimumConnectionCount-1).weight;
            currentThreshold = thresholdScore;
            for (int i = minimumConnectionCount; i < weightedEdges.size(); i++) {
                double weight = weightedEdges.get(i).weight;
                if (weight>currentThreshold){
                    currentThreshold = weight;
                    break;
                }

            }
            if (currentThreshold==thresholdScore) currentThreshold = 0;
//            currentThreshold = weightedEdges.get(this.minimumConnectionCount).weight;
        }

        if(currentThreshold < this.basicThreshold) {
            currentThreshold = this.basicThreshold;
        }


        for (WeightedEdge weightedEdge : weightedEdges) {
            if(weightedEdge.weight >= currentThreshold) {
                break;
            }
            graph.setLogWeight(weightedEdge.index1, weightedEdge.index2, currentThreshold - weightedEdge.weight); //todo correct ordering?
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

            for(int j = left; j <= right; ++j) {
                thresholds[j - left] = graph.getEdgeThreshold(j);
            }

            Arrays.sort(thresholds);
            int pos = Math.min(this.numberOfCandidatesWithMinConnCount, thresholds.length-1); //changed
//            double t = pos == 0?thresholds[0]:thresholds[thresholds.length-pos];
            //changed reverse order??
            double t = pos == 0?thresholds[thresholds.length-1]:thresholds[pos-1];
            if(t < this.basicThreshold) {
                throw new RuntimeException("individual edge threshold must not be smaller than overall threshold");
            }
//todo rather really look at the number of edges (may be less)
            for(int j = left; j <= right; ++j) {
                double current_t = graph.getEdgeThreshold(j);
                //todo check!! change to current_t>t ?
                //todo bug?!?! this may lead to 0 similar candidates getting a positive score. in principle it also should not be possible that current_t<t
                if(current_t > t) {
//                if(current_t != t) {
                    double diff = t - current_t;
                    int[] connections1 = graph.getLogWeightConnections(j);

                    for(int k = 0; k < connections1.length; ++k) {
                        int c = connections1[k];
                        double w = graph.getLogWeight(j, c);
                        graph.setLogWeight(j, c, Math.max(0.0D, w + diff));
                    }

                    graph.setEdgeThreshold(j, t);
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

//                if(max != 0.0D) { //changed
                if(max > 0.0D) {
                    connectionsList[i].add(j);
                    connectionsList[j].add(i);
                } else if (max < 0d) {
                    throw new RuntimeException("Edge has a negative weight");
                }
            }
        }

        int[][] connections = new int[graph.getSize()][];

        for(int i = 0; i < graph.getSize(); ++i) {
            connections[i] = connectionsList[i].toArray();
        }

        return connections;
    }

}
