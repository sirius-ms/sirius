
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ftalign.analyse;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ftalign.CSVMatrix;
import de.unijena.bioinf.treealign.Backtrace;
import de.unijena.bioinf.treealign.StackedBacktrace;
import de.unijena.bioinf.treealign.TreeAlignmentAlgorithm;
import de.unijena.bioinf.treealign.TreeAlignmentAlgorithm.Factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Kai Dührkop
 */
public class FTDataset {

    private final List<FTDataElement> lefts;
    private final List<FTDataElement> rights;
    private final Factory<Fragment> factory;
    private final boolean symetric;
    private final double[][] scoreMatrix;
    private volatile Backtrace<Fragment> tracer;
    private volatile Normalizer normalizer;
    private volatile List<BeforeCallback> beforeCallbacks;
    private volatile List<AfterCallback> afterCallbacks;
    private volatile boolean forceSelf;


    private FTDataset(List<FTDataElement> lefts, List<FTDataElement> rights, boolean symetric,
                      Factory<Fragment> factory) {
        this.lefts = new ArrayList<FTDataElement>(lefts);
        this.rights = new ArrayList<FTDataElement>(rights);
        this.scoreMatrix = new double[lefts.size()][rights.size()];
        for (double[] row : scoreMatrix) {
            Arrays.fill(row, Double.NaN);
        }
        this.symetric = symetric;
        this.factory = factory;
        this.tracer = null;
        this.beforeCallbacks = new ArrayList<FTDataset.BeforeCallback>();
        this.afterCallbacks = new ArrayList<FTDataset.AfterCallback>();
        this.forceSelf = false;
    }

    public FTDataset(List<FTDataElement> elements, Factory<Fragment> factory) {
        this(elements, elements, true, factory);
    }

    public FTDataset(List<FTDataElement> left, List<FTDataElement> right, Factory<Fragment> factory) {
        this(left, right, false, factory);
    }

    public boolean isForceSelf() {
        return forceSelf;
    }

    public void setForceSelf(boolean forceSelf) {
        this.forceSelf = forceSelf;
    }

    public int rows() {
        return lefts.size();
    }

    public int cols() {
        return rights.size();
    }

    public boolean isSymetric() {
        return symetric;
    }

    public FTDataElement getRowElement(int row) {
        return lefts.get(row);
    }

    public FTDataElement getColElement(int col) {
        return rights.get(col);
    }

    public Normalizer getNormalizer() {
        return normalizer;
    }

    public void setNormalizer(Normalizer normalizer) {
        this.normalizer = normalizer;
    }

    public void pushTracer(Backtrace<Fragment> newTracer) {
        if (newTracer == null) throw new IllegalArgumentException("expect non null value as argument");
        if (tracer != null && tracer instanceof StackedBacktrace) {
            ((StackedBacktrace<Fragment>) tracer).push(newTracer);
        } else if (tracer == null) {
            setTracer(newTracer);
        } else {
            final StackedBacktrace<Fragment> stack =
                    new StackedBacktrace<Fragment>();
            stack.push(tracer);
            stack.push(newTracer);
            tracer = stack;
        }
    }

    public CSVMatrix toCSV() {
        final double[][] matrixCopy = new double[lefts.size()][];
        for (int row = 0; row < lefts.size(); ++row) {
            matrixCopy[row] = Arrays.copyOf(scoreMatrix[row], scoreMatrix[row].length);
        }
        final String[] rowNames = new String[lefts.size()];
        final String[] colNames = new String[rights.size()];
        for (int i = 0; i < rowNames.length; ++i) {
            rowNames[i] = lefts.get(i).getName();
        }
        for (int i = 0; i < colNames.length; ++i) {
            colNames[i] = rights.get(i).getName();
        }
        return new CSVMatrix(rowNames, colNames, matrixCopy);
    }

    public Backtrace<Fragment> getTracer() {
        return tracer;
    }

    public void setTracer(Backtrace<Fragment> newTracer) {
        tracer = newTracer;
    }

    public void pushBeforeCallback(BeforeCallback callback) {
        this.beforeCallbacks.add(callback);
    }

    public List<BeforeCallback> getBeforeCallbacks() {
        return Collections.unmodifiableList(beforeCallbacks);
    }

    public void pushAfterCallback(AfterCallback callback) {
        this.afterCallbacks.add(callback);
    }

    public List<AfterCallback> getAfterCallback() {
        return Collections.unmodifiableList(afterCallbacks);
    }

    public void computeAllParallel() {
        // TODO: avaiable processor is not the best assumption for optimal thread count. Our program
        // has a big memory usage, maybe it is not such a good idea to let too many threads compute
        computeAllParallel(false, Runtime.getRuntime().availableProcessors());
    }

    public void computeAllParallel(final boolean forced, final int numberOfCPUs) {
        if (numberOfCPUs == 1) {
            computeAll(forced);
            return;
        } else if (numberOfCPUs < 1) {
            throw new IllegalArgumentException("illegal number of threads: " + numberOfCPUs);
        }
        final ArrayList<Future<?>> queue = new ArrayList<Future<?>>(lefts.size());
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfCPUs);
        for (int i = 0; i < lefts.size(); ++i) {
            queue.clear();
            for (int j = 0; j < rights.size(); ++j) {
                final int ivalue = i;
                final int jvalue = j;
                queue.add(executor.submit(new Callable<Object>() {
                    @Override
                    public Object call() {
                        if (forced) {
                            forceCompute(ivalue, jvalue);
                        } else {
                            compute(ivalue, jvalue);
                        }
                        return true;
                    }
                }));
            }
            for (Future<?> f : queue) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public void computeAll() {
        computeAll(false);
    }

    public void computeAll(final boolean forced) {
        for (int i = 0; i < lefts.size(); ++i) {
            for (int j = 0; j < rights.size(); ++j) {
                if (forced) {
                    forceCompute(i, j);
                } else {
                    compute(i, j);
                }
            }
        }
    }

    public double get(int i, int j) {
        return scoreMatrix[i][j];
    }

    public void computeFingerprints() {
        if (symetric) {
            computeSymetricFingerprints();
        } else {
            computeAssymetricFingerprints();
        }
    }

    private void computeAssymetricFingerprints() {
        // TODO: ist das wirklich dasselbe?
        computeSymetricFingerprints();
    }

    private void computeSymetricFingerprints() {
        final double[][] newMatrix = new double[lefts.size()][rights.size()];
        for (int i = 0; i < lefts.size(); ++i) {
            final double[] vectorLeft = scoreMatrix[i];
            for (int j = i; j < rights.size(); ++j) {
                final double[] vectorRight = scoreMatrix[j];
                final double pearson = Pearson.pearson(vectorLeft, vectorRight);
                newMatrix[i][j] = pearson;
                newMatrix[j][i] = pearson;
            }
        }
        for (int i = 0; i < scoreMatrix.length; ++i) {
            System.arraycopy(newMatrix[i], 0, scoreMatrix[i], 0, scoreMatrix[i].length);
        }
    }

    public double compute(int i, int j) {
        if (!Double.isNaN(scoreMatrix[i][j])) return scoreMatrix[i][j];
        if (i == j && symetric && !forceSelf) {
            final float score = factory.getScoring().selfAlignScore(lefts.get(i).getTree().getRoot());
            return set(i, i, normalize(lefts.get(i).getTree(), rights.get(i).getTree(), score));
        } else {
            return forceCompute(i, j);
        }
    }

    private double forceCompute(int i, int j) {
        final FTDataElement left = lefts.get(i);
        final FTDataElement right = rights.get(j);
        execBeforeCallback(left, right);
        final TreeAlignmentAlgorithm<Fragment> alg = factory.create(left.getTree().getRoot(), right.getTree().getRoot());
        final float origScore = alg.compute();

        final double score = normalize(left.getTree(), right.getTree(), origScore);
        set(i, j, score);
        if (tracer != null || afterCallbacks != null) {
            synchronized (this) {
                if (tracer != null) alg.backtrace(tracer);
                for (AfterCallback ac : afterCallbacks) ac.run(left, right, i, j, tracer, score);
            }
        }
        return score;
    }

    private void execBeforeCallback(FTDataElement left, FTDataElement right) {
        if (beforeCallbacks == null) return;
        synchronized (this) {
            for (BeforeCallback bc : beforeCallbacks) bc.run(left, right);
        }
    }

    private double set(int i, int j, double value) {
        synchronized (this) {
            scoreMatrix[i][j] = value;
            if (symetric && (i != j)) {
                scoreMatrix[j][i] = value;
            }
            return value;
        }
    }

    private double normalize(FTree left, FTree right, float score) {
        synchronized (this) {
            return normalizer == null ? score : normalizer.normalize(left, right, factory.getScoring(), score);
        }
    }

    public interface BeforeCallback {
        public void run(FTDataElement left, FTDataElement right);
    }

    public interface AfterCallback {
        public void run(FTDataElement left, FTDataElement right, int i, int j, Backtrace<Fragment> backtrace, double score);
    }


}
