/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class HierarchicalClustering<Taxon, InnerNode, PreMerge> {

    public BasicMasterJJob<InnerNode> makeParallelClusterJobs(final List<Taxon> entries) {
        return new BasicMasterJJob<InnerNode>(JJob.JobType.SCHEDULER) {

            @Override
            protected InnerNode compute() throws Exception {
                int n =  entries.size();
                final PreMerge[][] N = (PreMerge[][])new Object[n][n];


                List<InnerNode> clusters = entries.stream().map(x->submitSubJob(new BasicJJob<InnerNode>(){
                    @Override
                    protected InnerNode compute() throws Exception {
                        return createLeaf(x);
                    }
                })).map(JJob::takeResult).collect(Collectors.toList());
                final double[][] M = new double[n][n];
                for (int i=0; i < n; ++i) {
                    for (int j=0; j < i; ++j) {
                        final int I=i, J=j;
                        submitSubJob(new BasicJJob<PreMerge>() {
                            @Override
                            protected PreMerge compute() throws Exception {
                                N[I][J] = N[J][I] = preMerge(clusters.get(I), clusters.get(J));
                                return N[I][J];
                            }
                        });
                    }
                }
                awaitAllSubJobs();

                for (int i=0; i < n; ++i) {
                    for (int j=0; j < i; ++j) {
                        final int I=i, J=j;
                        submitSubJob(new BasicJJob<Double>() {
                            @Override
                            protected Double compute() throws Exception {
                                M[I][J] = M[J][I] = getScore(N[I][J], clusters.get(I), clusters.get(J));
                                return M[I][J];
                            }
                        });
                    }
                }
                awaitAllSubJobs();
                final int[] indizes = new int[n];
                for (int k=0; k < indizes.length; ++k)
                    indizes[k] = k;
                return upgmaJob(this, clusters, M, N, n, indizes);
            }
        };
    }

    public InnerNode cluster(List<Taxon> entries) {
        int n =  entries.size();
        final PreMerge[][] N = (PreMerge[][])new Object[n][n];
        List<InnerNode> clusters = entries.stream().map(this::createLeaf).collect(Collectors.toList());
        final double[][] M = new double[n][n];
        for (int i=0; i < n; ++i) {
            for (int j=0; j < i; ++j) {
                N[i][j] = N[j][i] = preMerge(clusters.get(i), clusters.get(j));
                M[i][j] = M[j][i] = getScore(N[i][j], clusters.get(i), clusters.get(j));
            }
        }
        final int[] indizes = new int[n];
        for (int k=0; k < indizes.length; ++k)
            indizes[k] = k;
        return upgma(clusters, M, N, n, indizes);
    }

    private InnerNode upgma(List<InnerNode> clusters, double[][] M, PreMerge[][] N, int n, int[] indizes)  {
        while (n > 1) {
            double maximum = Double.NEGATIVE_INFINITY;
            int bestI=0, bestJ=0;
            for (int i=0; i < n; ++i) {
                for (int j=0; j < i; ++j) {
                    final int I = indizes[i];
                    final int J = indizes[j];
                    if (I != J && M[I][J] > maximum) {
                        maximum = M[I][J];
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            // cluster!
            final InnerNode newNode = merge(N[indizes[bestI]][indizes[bestJ]], clusters.get(indizes[bestI]), clusters.get(indizes[bestJ]), maximum);
            // replace old
            final int deleteIndex = indizes[bestJ];
            final int newIndex = indizes[bestI];
            indizes[bestJ] = indizes[--n];
            clusters.set(newIndex, newNode);
            // recalculate distances
            for (int j=0; j < n; ++j) {
                final int index = indizes[j];
                if (index != newIndex) {
                    N[newIndex][index] = N[index][newIndex] = preMerge(newNode, clusters.get(index));
                    M[newIndex][index] = M[index][newIndex] = getScore(N[newIndex][index], newNode, clusters.get(index));
                }
            }
            for (int k=0; k < N.length; ++k) N[k][deleteIndex] = null;
            Arrays.fill(N[deleteIndex], null);
        }
        return clusters.get(indizes[0]);
    }

    private InnerNode upgmaJob(BasicMasterJJob<InnerNode> masterJob, List<InnerNode> clusters, double[][] M, PreMerge[][] N, int size, int[] indizes)  {
        BasicJJob<PreMerge>[] recalculations = (BasicJJob<PreMerge>[]) new BasicJJob[N.length];
        int n = size;
        while (n > 1) {
            double maximum = Double.NEGATIVE_INFINITY;
            int bestI=0, bestJ=0;
            for (int i=0; i < n; ++i) {
                for (int j=0; j < i; ++j) {
                    final int I = indizes[i];
                    final int J = indizes[j];
                    if (I != J && M[I][J] > maximum) {
                        maximum = M[I][J];
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            // cluster!
            final InnerNode newNode = merge(N[indizes[bestI]][indizes[bestJ]], clusters.get(indizes[bestI]), clusters.get(indizes[bestJ]), maximum);
            // replace old
            final int deleteIndex = indizes[bestJ];
            final int newIndex = indizes[bestI];
            indizes[bestJ] = indizes[--n];
            clusters.set(newIndex, newNode);
            // recalculate distances

            for (int j=0; j < n; ++j) {
                final int index = indizes[j];
                if (index != newIndex) {
                    recalculations[index] = masterJob.submitSubJob(new BasicJJob<PreMerge>() {
                        @Override
                        protected PreMerge compute() throws Exception {
                            return preMerge(newNode,clusters.get(index));
                        }
                    });
                }
            }
            for (int j=0; j < n; ++j) {
                final int index = indizes[j];
                if (index != newIndex) {
                    N[newIndex][index] = N[index][newIndex] = recalculations[index].takeResult();
                    M[newIndex][index] = M[index][newIndex] = getScore(N[newIndex][index], newNode, clusters.get(index));
                }
            }
            for (int k=0; k < N.length; ++k) N[k][deleteIndex] = null;
            Arrays.fill(N[deleteIndex], null);
        }
        return clusters.get(indizes[0]);
    }

    public abstract InnerNode createLeaf(Taxon entry);

    public PreMerge preMerge(InnerNode left, InnerNode right) {
        return null;
    }

    public abstract InnerNode merge(PreMerge preMerge, InnerNode left, InnerNode right, double score);

    public abstract double getScore(PreMerge preMerged, InnerNode left, InnerNode right);

}
