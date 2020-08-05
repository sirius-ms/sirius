
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

package de.unijena.bioinf.ftblast;

import de.unijena.bioinf.ChemistryBase.math.Statistics;

import java.util.*;

public class ScoreTable {

    private final double[][] matrix;
    private final String name;
    private Query[] ordered;
    private int numberOfHitsInterestedIn;

    public ScoreTable(String name, double[][] matrix) {
        this.matrix = matrix;
        this.name = name;
        this.numberOfHitsInterestedIn = -1;
    }

    public int getNumberOfHitsInterestedIn() {
        return numberOfHitsInterestedIn;
    }

    public void setNumberOfHitsInterestedIn(int numberOfHitsInterestedIn) {
        this.numberOfHitsInterestedIn = numberOfHitsInterestedIn;
    }

    public double[][] getMatrix() {
        return matrix;
    }

    public void toFingerprints(boolean spearman) {
        final double[][] copy = matrix.clone();
        for (int i = 0; i < copy.length; ++i) copy[i] = spearman ? toRank(matrix[i], 1e-3) : matrix[i].clone();
        for (int i = 0; i < copy.length; ++i) {
            matrix[i][i] = 1.0d;
            for (int j = i + 1; j < copy.length; ++j) {
                //extract(copy[i], xs, i, j);
                //extract(copy[j], ys, i, j);
                matrix[i][j] = Statistics.pearson(copy[i], copy[j]);
                matrix[j][i] = matrix[i][j];
            }
        }
        ordered = null;
    }


    private static double[] toRank(double[] xs, double delta) {
        final double[] rxs = xs.clone();
        final Integer[] stupid = new Integer[xs.length];
        for (int i = 0; i < stupid.length; ++i) stupid[i] = i;
        Arrays.sort(stupid, new Comparator<Integer>() {
            @Override
            public int compare(Integer i, Integer j) {
                return Double.compare(rxs[i], rxs[j]);
            }
        });
        for (int i = 0; i < rxs.length; ++i) {
            int j = i + 1;
            for (; j < rxs.length && (xs[stupid[j]] - xs[stupid[i]]) < delta; ++j) {
            }
            --j;
            for (int l = i; l <= j; ++l) {
                rxs[stupid[l]] = i + (j - i) / 2d;
            }
            i = j;
        }
        return rxs;
    }

    public void toFingerprints(int splitpoint) {
        final double[][] copy = matrix.clone();
        final double[] buffer1 = new double[copy.length - splitpoint], buffer2 = new double[copy.length - splitpoint];
        for (int i = 0; i < copy.length; ++i) copy[i] = matrix[i].clone();
        for (int i = 0; i < splitpoint; ++i) {
            matrix[i][i] = 1.0d;
            System.arraycopy(copy[i], 0, buffer1, 0, buffer1.length);
            for (int j = i + 1; j < splitpoint; ++j) {
                System.arraycopy(copy[j], 0, buffer2, 0, buffer2.length);
                matrix[i][j] = matrix[j][i] = Statistics.pearson(buffer1, buffer2);
            }
        }
        for (int i = splitpoint; i < matrix.length; ++i) {
            matrix[i][i] = 1.0d;
            System.arraycopy(copy[i], 0, buffer1, 0, buffer1.length);
            for (int j = i + 1; j < matrix.length; ++j) {
                System.arraycopy(copy[j], 0, buffer2, 0, buffer2.length);
                matrix[i][j] = matrix[j][i] = Statistics.pearson(buffer1, buffer2);
            }
        }
        ordered = null;
    }

    private void extract(double[] src, double[] dest, int i, int j) {
        final int a = Math.min(i, j);
        final int b = Math.max(i, j);
        if (a > 0) System.arraycopy(src, 0, dest, 0, a);
        System.arraycopy(src, a + 1, dest, a, b - a - 1);
        if (b < src.length) System.arraycopy(src, b + 1, dest, b - 1, dest.length - (b - 1));
    }

    public void filter(Predicate predicate) {
        getOrdered();
        final ArrayList<Query> queries = new ArrayList<Query>(ordered.length);
        for (Query q : ordered)
            if (predicate.isPassing(q)) queries.add(q);
        if (ordered != null) ordered = queries.toArray(new Query[queries.size()]);
    }


    public double scoreFor(Query q) {
        return matrix[q.row][q.col];
    }

    public double scoreFor(int row, int col) {
        return matrix[row][col];
    }

    public String getName() {
        return name;
    }

    public Query[] getOrdered() {
        if (ordered == null) {
            order();
        }
        return ordered;
    }

    private void order() {
        /*
        if (numberOfHitsInterestedIn < 0) orderAll();
        else order(numberOfHitsInterestedIn);
        */
        orderAll();
    }

    private void order(final int N) {
        final TreeSet<Query> queries = new TreeSet<Query>();
        double threshold = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < matrix.length; ++i) {
            for (int j = i + 1; j < matrix.length; ++j) {
                if (matrix[i][j] > threshold)
                    queries.add(new Query(i, j, matrix[i][j]));
            }
            while (queries.size() > N) {
                final Query lowest = queries.pollFirst();
                threshold = Math.max(threshold, lowest.score);
            }
        }
        this.ordered = queries.descendingSet().toArray(new Query[Math.min(queries.size(), N)]);
    }

    private void orderAll() {
        this.ordered = new Query[(matrix.length * matrix.length - matrix.length) / 2];
        int k = 0;
        for (int i = 0; i < matrix.length; ++i) {
            for (int j = i + 1; j < matrix.length; ++j) {
                //if (i==j) continue;
                ordered[k++] = new Query(i, j, matrix[i][j]);
            }
        }
        ordered = Arrays.copyOf(ordered, k);
        Arrays.sort(ordered, Collections.reverseOrder());
    }

    public void map(Function f) {
        for (int i = 0; i < matrix.length; ++i) {
            for (int j = i; j < matrix.length; ++j) {
                final double newScore = f.map(new Query(i, j, matrix[i][j]));
                matrix[i][j] = newScore;
                matrix[j][i] = newScore;
            }
        }
        ordered = null;
    }

    public double average() {
        double r = 0d;
        for (int i = 0; i < matrix.length; ++i) {
            for (int j = 0; j < matrix[i].length; ++j) {
                r += matrix[i][j];
            }
        }
        return r / (matrix.length * matrix[0].length);
    }

    public static interface Predicate {
        public boolean isPassing(Query q);
    }

    public static interface Function {
        public double map(Query q);
    }
}
