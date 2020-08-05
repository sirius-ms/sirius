
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

package de.unijena.bioinf.treealign;

import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.treealign.dp.DPTreeAlign;
import de.unijena.bioinf.treealign.multijoin.DPMultiJoin;
import de.unijena.bioinf.treealign.scoring.Scoring;
import de.unijena.bioinf.treealign.sparse.DPSparseTreeAlign;

/**
 * @author Kai Dührkop
 */
public interface TreeAlignmentAlgorithm<T> {

    public float compute();

    public void backtrace(Backtrace<T> tracer);

    public static class Factory<T> {

        protected final int numberOfJoins;
        protected final boolean many2manyJoins;
        protected final TreeAdapter<T> adapter;
        protected final Scoring<T> scoring;

        public Factory(TreeAdapter<T> adapter, Scoring<T> scoring,
                       int numberOfJoins, boolean many2manyJoins) {
            this.numberOfJoins = numberOfJoins;
            this.many2manyJoins = many2manyJoins;
            this.adapter = adapter;
            this.scoring = scoring;
        }

        public int getNumberOfJoins() {
            return numberOfJoins;
        }

        public boolean isMany2manyJoins() {
            return many2manyJoins;
        }

        public TreeAdapter<T> getAdapter() {
            return adapter;
        }

        public Scoring<T> getScoring() {
            return scoring;
        }

        public TreeAlignmentAlgorithm<T> create(T left, T right) {
            if (numberOfJoins == 0) {
                return new DPSparseTreeAlign<T>(scoring, false, left, right, adapter);
            } else if (numberOfJoins == 1) {
                if (many2manyJoins) {
                    return new DPMultiJoin<T>(scoring, 1, left, right, adapter);
                } else {
                    return new DPSparseTreeAlign<T>(scoring, true, left, right, adapter);
                }
            } else {
                return new DPMultiJoin<T>(scoring, numberOfJoins, left, right, adapter);
            }
        }

    }

    public static class NonSparseFactory<T> extends Factory<T> {

        public NonSparseFactory(TreeAdapter<T> adapter, Scoring<T> scoring, boolean useJoins) {
            super(adapter, scoring, useJoins ? 1 : 0, false);
        }

        @Override
        public TreeAlignmentAlgorithm<T> create(T left, T right) {
            return new DPTreeAlign<T>(scoring, numberOfJoins > 0, left, right, adapter);
        }

    }


}
