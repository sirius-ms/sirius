package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class KMeans<T> {

    public interface Centroiding<T> {
        public T findCentroid(List<T> datapoints);
    }

    public interface Metric<T> {
        public double distanceBetween(T left, T right);
    }

    public static KMeans<double[]> forDouble() {
        return new KMeans<>(new Centroiding<double[]>() {
            @Override
            public double[] findCentroid(List<double[]> datapoints) {
                double[] centroid = datapoints.get(0).clone();
                for (int i = 1; i < datapoints.size(); ++i) {
                    final double[] vec = datapoints.get(i);
                    for (int j = 0; j < centroid.length; ++j) {
                        centroid[j] += vec[j];
                    }
                }
                for (int j = 0; j < centroid.length; ++j) {
                    centroid[j] /= datapoints.size();
                }
                return centroid;
            }
        }, new Metric<double[]>() {
            @Override
            public double distanceBetween(double[] left, double[] right) {
                double eucl = 0d;
                for (int i=0; i < left.length; ++i) {
                    eucl += (left[i]-right[i])*(left[i]-right[i]);
                }
                return eucl;
            }
        });
    }

    private final Centroiding<T> centroiding;
    private final Metric<T> metric;
    private int maxIterations=20;
    private Long randomSeed=null;

    public KMeans(Centroiding<T> centroiding, Metric<T> metric) {
        this.centroiding = centroiding;
        this.metric = metric;
    }

    public Clustering cluster(List<T> dataPoints, int nclusters) {
        final Clustering clustering = new Clustering(dataPoints);
        if (randomSeed!=null) clustering.random.setSeed(randomSeed);
        clustering.cluster(nclusters, maxIterations);
        return clustering;
    }

    public BasicJJob<Clustering> clusterJob(List<T> dataPoints, int nclusters) {
        final Clustering clustering = new Clustering(dataPoints);
        if (randomSeed!=null) clustering.random.setSeed(randomSeed);
        return clustering.makeJob(nclusters, maxIterations);
    }

    public class Clustering {
        private final List<T> dataPoints;
        private final List<T> centroids;
        private final List<List<T>> assignments;
        private Random random;
        private int[] assignment;

        private Clustering(List<T> dataPoints) {
            this.dataPoints = dataPoints;
            this.centroids = new ArrayList<>();
            this.assignments = new ArrayList<>();
            this.assignment = new int[dataPoints.size()];
            this.random = new Random();
        }

        public int[] getAssignments() {
            return assignment;
        }

        public List<T> getCentroids() {
            return new ArrayList<>(centroids);
        }

        public T getClusterCentroid(int k) {
            return centroids.get(k);
        }

        public List<T> getClusterData(int k) {
            return assignments.get(k);
        }

        private void randomStart(int nclusters) {
            centroids.clear();
            if (nclusters >= Math.floor(dataPoints.size() * 0.1)) {
                ArrayList<T> xs = new ArrayList<>(dataPoints);
                Collections.shuffle(xs, random);
                centroids.addAll(xs.subList(0, nclusters));
            } else {
                final TIntHashSet indizes = new TIntHashSet();
                while (indizes.size() < nclusters) {
                    indizes.add(random.nextInt(dataPoints.size()));
                }
                indizes.forEach(i -> {
                    centroids.add(dataPoints.get(i));
                    return true;
                });
            }
            assignments.clear();
            for (int i = 0; i < nclusters; ++i)
                assignments.add(new ArrayList<>());
        }

        private void cluster(int nclusters, int maxIterations) {
            if (nclusters <= 1 || nclusters >= dataPoints.size()) {
                throw new IllegalArgumentException();
            }
            randomStart(nclusters);
            int repeatsWithoutChange = 0;
            for (int i = 0; i < maxIterations; ++i) {
                if (assign()) {
                    repeatsWithoutChange = 0;
                } else {
                    if (++repeatsWithoutChange > 2) break;
                }
                relocate();
            }
        }

        private void relocate() {
            for (int j = 0; j < centroids.size(); ++j) {
                centroids.set(j, centroiding.findCentroid(assignments.get(j)));
            }
        }

        private boolean assign() {
            boolean hasChanged = false;
            for (List<T> as : assignments) as.clear();
            for (int i = 0; i < dataPoints.size(); ++i) {
                final T P = dataPoints.get(i);
                double minDistance = Double.POSITIVE_INFINITY;
                int minindex = 0;
                for (int j = 0; j < centroids.size(); ++j) {
                    final double d = metric.distanceBetween(centroids.get(j), P);
                    if (d < minDistance) {
                        minDistance = d;
                        minindex = j;
                    }
                }
                assignments.get(minindex).add(P);
                if (assignment[i] != minindex) {
                    hasChanged = true;
                    assignment[i] = minindex;
                }
            }
            return hasChanged;
        }

        public BasicMasterJJob<Clustering> makeJob(int nclusters, int maxIterations) {
            throw new NotImplementedException();
        }
    }
}
