package de.unijena.bioinf.utils.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * cluster elements and provides final clusters. clustering is stopped at threshold.
 * //todo currently the hierarchy is not stored.
 * @param <T>
 */
public class HierarchicalClustering<T> {

    private DistanceMeasureStrategy distanceMeasureStrategy;
    private T[] elements;
    private double[][] inputDistanceMatrix;
    private ClusteringMatrix distances;
    private List<List<T>> clusters;

    public HierarchicalClustering(DistanceMeasureStrategy distanceMeasureStrategy) {
        this.distanceMeasureStrategy = distanceMeasureStrategy;
    }

    public void cluster(T[] elements, double[][] pairwiseDistances) {
        cluster(elements, pairwiseDistances, Double.POSITIVE_INFINITY);
    }

    public void cluster(T[] elements, double[][] pairwiseDistances, double threshold) {
        this.elements = elements;
        this.inputDistanceMatrix = pairwiseDistances;
        this.distances = new ClusteringMatrixImplementation(pairwiseDistances);
        this.clusters = new ArrayList<>();
        for (T element : elements) {
            List<T> list = new ArrayList<>();
            list.add(element);
            clusters.add(list);
        }
        while (clusters.size()>1){
            IndexPair minimum = distances.getMinimum();
            if (minimum==null || distances.getDistance(minimum.i, minimum.j).getDistance()>threshold){
                //no elements left to cluster
                return;
            }

            int clusterSize1 = clusters.get(minimum.i).size();
            int clusterSize2 = clusters.get(minimum.j).size();

            Distance[] newDistancesRow = new Distance[distances.numberOfClusters()-1];
            int newIdx = 0;
            for (int i = 0; i < distances.numberOfClusters()-1; i++) {
                int idx = i<minimum.j?i:i+1;
                Distance d;
                if (idx==minimum.i){
                    d = new Distance(0d);
                } else {
                    Distance d1 = distances.getDistance(idx, minimum.i);
                    Distance d2 = distances.getDistance(idx, minimum.j);
                    d = distanceMeasureStrategy.calcNewDistance(d1, d2, clusterSize1, clusterSize2);
                }
                newDistancesRow[newIdx++] = d;
            }
            distances.update(minimum, newDistancesRow);

            clusters.get(minimum.i).addAll(clusters.get(minimum.j));
            clusters.remove(minimum.j);

        }
    }

    public List<List<T>> getClusters() {
        return clusters;
    }

    private static class IndexPair {
        final int i;
        final int j;

        private IndexPair(int i, int j) {
            this.i = i;
            this.j = j;
        }
    }

    private interface ClusteringMatrix {

        public int numberOfClusters();

        public Distance getDistance(int clusterIdx1, int clusterIdx2);

        public Distance[] getDistances(int clusterIdx);

        public Distance[][] getDistances();

        public IndexPair getMinimum();

        /*
        updated elements will be indexed with minimum index of indexPair
         */

        /**
         * updated elements will be indexed with minimum index of indexPair
         * @param indexPair
         * @param newDistancesRow updated distances. i-th position is 0.
         *                        Assumes that the j-th element of last step is already removed.
         */
        public void update(IndexPair indexPair, Distance[] newDistancesRow);
    }


    private class ClusteringMatrixImplementation implements ClusteringMatrix {

        Distance[][] distances;

        public ClusteringMatrixImplementation(double[][] pairwiseDistances) {
            //todo test triangle property
            distances = new Distance[pairwiseDistances.length][];
            for (int i = 0; i < pairwiseDistances.length; i++) {
                double[] pairwiseDistance = pairwiseDistances[i];
                distances[i] = new Distance[pairwiseDistances.length-i];
                for (int j = i; j < pairwiseDistance.length; j++) {
                    distances[i][j-i] = new Distance(pairwiseDistance[j]);
                }
            }
        }

        @Override
        public int numberOfClusters() {
            return distances.length;
        }

        @Override
        public Distance getDistance(int clusterIdx1, int clusterIdx2) {
            if (clusterIdx1<=clusterIdx2){
                return distances[clusterIdx1][clusterIdx2-clusterIdx1];
            }
            return distances[clusterIdx2][clusterIdx1-clusterIdx2];
        }

        @Override
        public Distance[] getDistances(int clusterIdx) {
            Distance[] distancesRow = new Distance[distances.length];
            for (int i = 0; i < clusterIdx; i++) {
                distancesRow[i] = distances[i][clusterIdx];
            }
            for (int i = clusterIdx; i < distances.length; i++) {
                distancesRow[i] = distances[clusterIdx][i-clusterIdx];
            }
            return distancesRow;
        }

        @Override
        public Distance[][] getDistances() {
            return distances;
        }

        @Override
        public IndexPair getMinimum() {
            int minI = 0, minJ = 1;
            Distance min = distances[0][1];
            for (int i = 0; i < distances.length; i++) {
                Distance[] distance = distances[i];
                for (int j = 1; j < distance.length; j++) {
                    Distance d = distance[j];
                    if (Double.isNaN(min.getDistance()) || d.getDistance()<min.getDistance()){
                        minI = i;
                        minJ = j;
                        min = d;
                    }
                }
            }
            if (Double.isNaN(min.getDistance()) || min.getDistance()==Double.POSITIVE_INFINITY) return null;
            return new IndexPair(minI, minI+minJ);
        }

        @Override
        public void update(IndexPair indexPair, Distance[] newDistancesRow) {
            // i-th entry will be updated to the new distance, j-th entry will be removed;
            int iPos = Math.min(indexPair.i, indexPair.j);
            int jPos = Math.max(indexPair.i, indexPair.j);
            Distance[][] newDistances = new Distance[distances.length-1][];

            for (int pos = 0; pos < jPos; pos++) {
                if (iPos==pos) {
                    Distance[] newDistance = Arrays.copyOfRange(newDistancesRow, pos, newDistancesRow.length);
                    newDistances[pos] = newDistance;
                    assert newDistance.length==distances.length-pos-1;
                    continue;
                }
                Distance[] distance = distances[pos];
                Distance[] newDistance = removeElement(distance, jPos-pos);
                if (iPos>pos){
                    newDistance[iPos-pos] = newDistancesRow[pos];
                }
                newDistances[pos] = newDistance;
            }
            for (int pos = jPos+1; pos < distances.length; pos++) {
                Distance[] newDistance = distances[pos];
//                newDistance[iPos-pos] = newDistancesRow[pos];
                newDistances[pos-1] = newDistance;
            }

            distances = newDistances;
            assert isDiagonalMatrix(distances);
        }

        private Distance[] removeElement(Distance[] array, int idx){
            Distance[] n = new Distance[array.length-1];
            for (int i = 0; i < idx; i++) {
                n[i] = array[i];
            }
            for (int i = idx+1; i < array.length; i++) {
                n[i-1] = array[i];
            }
            return n;
        }

        private boolean isDiagonalMatrix(Distance[][] matrix){
            for (int i = 0; i < matrix.length-1; i++) {
                if (matrix[i].length-matrix[i+1].length!=1) return false;
            }
            return true;
        }
    }
}
