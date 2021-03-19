
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.TreeSet;

public class Dataset {

    private final ScoreTable trueScores;
    private final ArrayList<ScoreTable> otherScores;
    private final double random;
    private final int maxK;

    public Dataset(ScoreTable trueScores, int maxK) {
        this.trueScores = trueScores;
        this.maxK = maxK;
        this.otherScores = new ArrayList<ScoreTable>();
        random = trueScores.average();
        setMaxKFor(trueScores);
    }

    public static double ssps(ScoreTable trueScores, ScoreTable scores, final float k) {
        return calculateSSPS(trueScores, k, scores.getOrdered());
    }

    private static double calculateSSPS(ScoreTable trueScores, float k, Query[] qs) {
        final double n = trueScores.getMatrix().length;
        final double lhs = 2d / (k * k * (n - 1) * (n - 1) + k * (n - 1));
        double rhs = 0d;
        for (int j = 0; j < k * (n - 1); ++j) {
            rhs += (k * (n - 1) - j) * trueScores.scoreFor(qs[j]);
            assert trueScores.scoreFor(qs[j]) >= 0;
            assert trueScores.scoreFor(qs[j]) <= 1;
        }
        return lhs * rhs;
    }

    private static double[] calculateCorrelation(ScoreTable trueScores, int maxk, ScoreTable table) {
        final double[][] matrix = table.getMatrix();
        final int minSize = maxk + 1;
        final double[] result = new double[minSize];
        final TreeSet<Query> queue = new TreeSet<Query>();
        for (int i = 0; i < matrix.length; ++i) {
            double minScore = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < matrix[i].length; ++j) {
                if (i != j && matrix[i][j] > minScore) {
                    queue.add(new Query(i, j, matrix[i][j]));
                    if (queue.size() > minSize) {
                        queue.pollFirst();
                        minScore = Math.max(minScore, queue.first().score);
                    }
                }
            }
            int k = 0;
            double sum = 0d;
            for (Query q : queue.descendingSet()) {
                sum += trueScores.scoreFor(q);
                result[k] += sum / (k + 1d);
                ++k;
            }
            queue.clear();
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] /= matrix.length;
        }
        return result;
    }

    private static double[] calculateCorrelationCrossDB(ScoreTable trueScores, int maxk, ScoreTable table, int splitpoint) {
        final double[][] matrix = table.getMatrix();
        final int minSize = maxk + 1;
        final double[] result = new double[minSize];
        final TreeSet<Query> queue = new TreeSet<Query>();
        for (int i = 0; i < matrix.length; ++i) {
            double minScore = Double.NEGATIVE_INFINITY;
            int start;
            int ende;
            if (i >= splitpoint) {
                start = 0;
                ende = splitpoint;
            } else {
                start = splitpoint;
                ende = matrix[0].length;
            }
            for (int j = start; j < ende; ++j) {
                if (matrix[i][j] > minScore) {
                    queue.add(new Query(i, j, matrix[i][j]));
                    if (queue.size() > minSize) {
                        queue.pollFirst();
                        minScore = Math.max(minScore, queue.first().score);
                    }
                }
            }
            int k = 0;
            double sum = 0d;
            for (Query q : queue.descendingSet()) {
                sum += trueScores.scoreFor(q);
                result[k] += sum / (k + 1d);
                ++k;
            }
            queue.clear();
        }
        for (int i = 0; i < result.length; ++i) {
            result[i] /= matrix.length;
        }
        return result;
    }

    private void setMaxKFor(ScoreTable scores) {
        scores.setNumberOfHitsInterestedIn(scores.getMatrix().length * maxK);
    }

    public void add(ScoreTable table) {
        this.otherScores.add(table);
        setMaxKFor(table);
    }

    public ScoreTable getTable(String name) {
        if (trueScores.getName().equals(name)) return trueScores;
        for (ScoreTable s : otherScores) if (s.getName().equals(name)) return s;
        return null;
    }

    public double ssps(String name, int k) {
        if (name.equals("random")) return sspsAverageRandom();
        if (name.equals(trueScores.getName())) return sspsOpt(k);
        for (ScoreTable t : otherScores)
            if (name.equals(t.getName())) return ssps(trueScores, t, k);
        throw new NoSuchElementException("Unknown score table '" + name + "'");
    }

    public double sspsOpt(float k) {
        return ssps(trueScores, trueScores, k);
    }

    public double sspsRealRandom(int k) {
        final Query[] qs = Arrays.copyOf(trueScores.getOrdered(), trueScores.getOrdered().length);
        Statistics.shuffle(qs);
        return calculateSSPS(trueScores, k, qs);
    }

    public double sspsAverageRandom() {
        return random;
    }

    public double[] averageChemicalSimilarity(ScoreTable scores, final int maxk) {
        return calculateCorrelation(trueScores, maxk, scores);
    }

    public double[] averageChemicalSimilarityCrossDB(ScoreTable scores, final int maxk, int splitpoint) {
        return calculateCorrelationCrossDB(trueScores, maxk, scores, splitpoint);
    }

    public double ssps(ScoreTable scores, final float k) {
        return calculateSSPS(trueScores, k, scores.getOrdered());
    }

    public void filterOutIdenticalCompounds(String reference1, String reference2) {
        final ScoreTable ref1 = getTable(reference1);
        final ScoreTable ref2 = getTable(reference2);
        filterOutIdenticalCompounds(trueScores, ref1, ref2);
        for (ScoreTable t : otherScores) {
            filterOutIdenticalCompounds(t, ref1, ref2);
        }
    }

    public void allowOnlyCompoundsFromDifferentDatasets(final int splitpoint) {
        final ScoreTable.Predicate predicate = new ScoreTable.Predicate() {
            @Override
            public boolean isPassing(Query q) {
                return (q.col >= splitpoint && q.row < splitpoint) || (q.col < splitpoint && q.row >= splitpoint);
            }
        };
        trueScores.filter(predicate);
        for (ScoreTable t : otherScores) t.filter(predicate);
    }

    // removes all Hits (u,u) and all hits (u,v) with u and v are the same chemical molecule
    // this is done by using two tanimoto scores. If both are 1, then it is very(!) likely that the compounds
    // are also equal
    private void filterOutIdenticalCompounds(final ScoreTable table, final ScoreTable ref1, final ScoreTable ref2) {
        table.filter(new ScoreTable.Predicate() {
            @Override
            public boolean isPassing(Query q) {
                if (ref1.scoreFor(q) >= 0.99999 && ref2.scoreFor(q) >= 0.99999) return false;
                return true;
            }
        });
    }


}
