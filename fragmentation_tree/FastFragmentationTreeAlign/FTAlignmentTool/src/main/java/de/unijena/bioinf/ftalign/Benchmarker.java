
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

package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ftalign.analyse.FTDataElement;
import de.unijena.bioinf.treealign.TreeAlignmentAlgorithm;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Benchmarker {

    public static long benchmarkCompleteTime(final List<FTDataElement> elements, final TreeAlignmentAlgorithm.Factory<Fragment> factory, final int repeats, int numberOfCores) {
        long bestTime = Long.MAX_VALUE;
        for (int t = 0; t < repeats; ++t) {
            final ExecutorService service = Executors.newFixedThreadPool(numberOfCores);
            final long time1 = System.nanoTime();
            final int n = elements.size();
            for (int i = 0; i < n; ++i) {
                final int i_ = i;
                service.submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        for (int j = i_ + 1; j < n; ++j) {
                            factory.create(elements.get(i_).getTree().getRoot(), elements.get(j).getTree().getRoot()).compute();
                        }
                        return null;
                    }
                });
            }
            service.shutdown();
            try {
                service.awaitTermination(1000, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final long time2 = System.nanoTime();
            final long runtime = time2 - time1;
            if (runtime < bestTime) {
                bestTime = runtime;
            }
        }
        return bestTime;
    }

    public static long benchmarkCompleteTime(final List<FTDataElement> lefts, final List<FTDataElement> rights, final TreeAlignmentAlgorithm.Factory<Fragment> factory, final int repeats, int numberOfCores) {
        long bestTime = Long.MAX_VALUE;
        for (int t = 0; t < repeats; ++t) {
            final ExecutorService service = Executors.newFixedThreadPool(numberOfCores);
            final long time1 = System.nanoTime();
            for (final FTDataElement left : lefts) {
                service.submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        for (final FTDataElement right : rights) {
                            factory.create(left.getTree().getRoot(), right.getTree().getRoot()).compute();
                        }
                        return null;
                    }
                });
            }
            service.shutdown();
            try {
                service.awaitTermination(1000, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final long time2 = System.nanoTime();
            final long runtime = time2 - time1;
            if (runtime < bestTime) {
                bestTime = runtime;
            }
        }
        return bestTime;
    }

    public static double[][] benchmark(List<FTDataElement> elements, TreeAlignmentAlgorithm.Factory<Fragment> factory, int repeats) {
        final int n = elements.size();
        final double[][] measurement = new double[2][(n * n - n) / 2];
        int l = 0;
        for (int i = 0; i < n; ++i) {
            for (int j = i + 1; j < n; ++j) {
                long bestTime = Long.MAX_VALUE;
                double score = 0d;
                for (int k = 0; k < repeats; ++k) {
                    final long time = System.nanoTime();
                    factory.create(elements.get(i).getTree().getRoot(), elements.get(j).getTree().getRoot()).compute();
                    final long runtime = System.nanoTime() - time;
                    if (runtime < bestTime) bestTime = runtime;
                }
                measurement[0][l] = bestTime;
                measurement[1][l++] = score;
            }
        }
        return measurement;
    }

    public static double[][] benchmark(List<FTDataElement> lefts, List<FTDataElement> rights, TreeAlignmentAlgorithm.Factory<Fragment> factory, int repeats) {
        final int n1 = lefts.size();
        final int n2 = rights.size();
        final double[][] measurement = new double[2][n1 * n2];
        int l = 0;
        for (int i = 0; i < n1; ++i) {
            for (int j = i + 1; j < n2; ++j) {
                long bestTime = Long.MAX_VALUE;
                double score = 0d;
                for (int k = 0; k < repeats; ++k) {
                    final long time = System.nanoTime();
                    factory.create(lefts.get(i).getTree().getRoot(), rights.get(j).getTree().getRoot()).compute();
                    final long runtime = System.nanoTime() - time;
                    if (runtime < bestTime) bestTime = runtime;
                }
                measurement[0][l] = bestTime;
                measurement[1][l++] = score;
            }
        }
        return measurement;
    }

}
