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

package de.unijena.bioinf.retention;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.retention.kernels.MoleculeKernel;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class RetentionOrderDataset {

    private ArrayList<PredictableCompound> compounds;
    private TObjectIntHashMap<PredictableCompound> compounds2index;
    private ArrayList<PredictableCompound> relations;
    private BitSet mask;
    private int[] indizes;

    public RetentionOrderDataset() {
        this.compounds = new ArrayList<>();
        this.compounds2index = new TObjectIntHashMap<>();
        this.relations = new ArrayList<>();
        this.mask = new BitSet();
    }

    public int[] getPairs() {
        return relations.stream().mapToInt(r->compounds2index.get(r)).toArray();
    }

    private RetentionOrderDataset(ArrayList<PredictableCompound> compounds, TObjectIntHashMap<PredictableCompound> compounds2index, ArrayList<PredictableCompound> relations, BitSet mask) {
        this.compounds = compounds;
        this.compounds2index = compounds2index;
        this.relations = relations;
        this.mask = mask;
    }

    protected RetentionOrderDataset shallowCopy() {
        return new RetentionOrderDataset(compounds,compounds2index,relations,(BitSet)mask.clone());
    }

    public void addCompound(PredictableCompound compound) {
        compounds2index.put(compound, compounds.size());
        mask.set(compounds.size(), true);
        compounds.add(compound);
    }

    public void addRelation(PredictableCompound left, PredictableCompound right) {
        relations.add(left);
        relations.add(right);
    }

    public <T> BasicJJob<List<T>> prepareTrainingCompounds(MoleculeKernel<T> kernelFunction) {
        return new BasicMasterJJob<List<T>>(JJob.JobType.SCHEDULER) {
            @Override
            protected List<T> compute() throws Exception {
                final int[] indizes = getUsedIndizes();
                final List<BasicJJob<T>> jobs = Arrays.stream(indizes).mapToObj(i->submitSubJob(new BasicJJob<T>() {
                    @Override
                    protected T compute() throws Exception {
                        return kernelFunction.prepare(compounds.get(i));
                    }
                })).collect(Collectors.toList());
                return jobs.stream().map(JJob::takeResult).collect(Collectors.toList());
            }
        };
    }

    public <T> BasicJJob<double[]> computeTestKernel(MoleculeKernel<T> kernelFunction, List<T> prepared, PredictableCompound testCompound) {
        return new BasicMasterJJob<double[]>(JJob.JobType.SCHEDULER) {
            @Override
            protected double[] compute() throws Exception {
                final int[] indizes = getUsedIndizes();
                final double[] kernel = new double[indizes.length];
                final T preparedTest = submitSubJob(new BasicJJob<T>() {
                    @Override
                    protected T compute() throws Exception {
                        return kernelFunction.prepare(testCompound);
                    }
                }).takeResult();
                for (int k=0; k < kernel.length; ++k) {
                    final int K = k;
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            final PredictableCompound compound = compounds.get(indizes[K]);
                            final T preparedTrain = prepared.get(K);
                            kernel[K] = kernelFunction.compute(testCompound,compound,preparedTest,preparedTrain);
                            return true;
                        }
                    });
                }
                return kernel;
            }
        };
    }

    public <T> BasicJJob<double[]> computeTestKernel(MoleculeKernel<T> kernelFunction, PredictableCompound testCompound) {
        return new BasicMasterJJob<double[]>(JJob.JobType.CPU) {
            @Override
            protected double[] compute() throws Exception {
                final int[] indizes = getUsedIndizes();
                final double[] kernel = new double[indizes.length];
                final T prepared = kernelFunction.prepare(testCompound);
                for (int k=0; k < kernel.length; ++k) {
                    final int K = k;
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            final PredictableCompound compound = compounds.get(indizes[K]);
                            final T preparedTrain = kernelFunction.prepare(compound);
                            kernel[K] = kernelFunction.compute(testCompound,compound,prepared,preparedTrain);
                            return true;
                        }
                    });
                }
                awaitAllSubJobs();
                return kernel;
            }
        };
    }

    public <T>BasicJJob<double[][]> computeTrainKernel(MoleculeKernel<T> kernel) {
        return new BasicMasterJJob<double[][]>(JJob.JobType.SCHEDULER){
            @Override
            protected double[][] compute() throws Exception {
                final int[] indizes = getUsedIndizes();
                PredictableCompound[] cmps = Arrays.stream(indizes).mapToObj(i->compounds.get(i)).toArray(PredictableCompound[]::new);
                BasicJJob<T>[] preparedJobs = Arrays.stream(cmps).map(c->submitSubJob(new BasicJJob<T>() {
                    @Override
                    protected T compute() throws Exception {
                        return kernel.prepare(c);
                    }
                })).toArray(BasicJJob[]::new);
                final List<T> prepared = new ArrayList<>(preparedJobs.length);
                for (BasicJJob<T> t : preparedJobs) {
                    prepared.add(t.takeResult());
                }

                final double[][] M = new double[indizes.length][indizes.length];
                final int middle = indizes.length>>1;
                for (int k=0; k < middle; ++k) {
                    final int ROW = k;
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            final int SECONDROW = M.length-ROW-1;
                            for (int col=0; col < ROW; ++col) {
                                M[ROW][col]  = M[col][ROW] = kernel.compute(cmps[ROW],cmps[col],prepared.get(ROW),prepared.get(col));
                            }
                            for (int col=0; col < SECONDROW; ++col) {
                                M[SECONDROW][col]  = M[col][SECONDROW] = kernel.compute(cmps[SECONDROW],cmps[col],prepared.get(SECONDROW),prepared.get(col));
                            }
                            return true;
                        }
                    });
                }
                submitSubJob(new BasicJJob<Object>() {
                    @Override
                    protected Object compute() throws Exception {
                        for (int i=0; i < M.length; ++i) {
                            M[i][i] = kernel.compute(cmps[i],cmps[i], prepared.get(i),prepared.get(i));
                        }
                        return true;
                    }
                });
                if (M.length%2!=0) {
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            for (int col = 0; col < middle; ++col) {
                                M[middle][col]  = M[col][middle] = kernel.compute(cmps[middle],cmps[col],prepared.get(middle),prepared.get(col));
                            }
                            return true;
                        }
                    });
                }
                awaitAllSubJobs();
                return M;
            }
        };
    }

    public int[] getUsedIndizes() {
        if (indizes!=null) return indizes;
        int[] indizes = new int[mask.cardinality()];
        int k=0;
        for (int i=mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i+1)) {
            indizes[k++] = i;
        }
        this.indizes = indizes;
        return indizes;
    }

    public void useAllCompounds() {
        indizes=null;
        mask.set(0,compounds.size(), true);
    }

    public void useNoCompounds() {
        indizes=null;
        mask.set(0,compounds.size(), true);
    }

    public void useCompound(int index, boolean allow) {
        indizes=null;
        mask.set(index,allow);
    }
    public void useCompound(PredictableCompound compound, boolean allow) {
        indizes=null;
        mask.set(compounds2index.get(compound),allow);
    }

    public void useEveryXCompound(int fold, boolean value) {
        indizes=null;
        mask.set(0,compounds.size(),!value);
        for (int k=0; k < compounds.size(); k += fold) {
            mask.set(k,value);
        }
    }


}
