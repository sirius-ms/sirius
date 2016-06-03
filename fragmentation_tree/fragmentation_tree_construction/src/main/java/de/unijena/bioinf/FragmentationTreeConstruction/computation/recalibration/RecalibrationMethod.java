/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;
import org.apache.commons.math3.analysis.UnivariateFunction;

public interface RecalibrationMethod extends Parameterized {

    /**
     * recalibrate fragmentation tree. It is recommended to implement this method lazy such that it return just an empty
     * Recalibration object. The recalibration itself can then be executed after accessing the #getScoreBonus or
     * #getCorrectedTree method.
     *
     * @return recalibration 'method' object
     */
    public Recalibration recalibrate(FTree tree, MassDeviationVertexScorer scorer, boolean force);

    /**
     * Return value of a recalibration. Can be implemented lazy (recalibrate as soon as the recalibrated properties
     * are accessed).
     */
    public interface Recalibration {

        /**
         * Returns the score bonus of the recalibration without recomputing the tree. The Score bonus expects a MassDeviationVertexScorer
         * (or subclass) for mass deviation scoring. The score bonus is computed by recalibration peak masses of all vertices
         * and subtract the new score from the old one.
         */
        public double getScoreBonus();

        /**
         * @return a hint if it is necessary to recompute the tree. Recomputing takes additional time (but not much,
         * as there is a strict LP_LOWERBOUND) but may give you more score bonus.
         */
        public boolean shouldRecomputeTree();

        /**
         * Recompute the tree and return the corrected tree
         *
         * @param analyzer the analyzer pipeline that should be used for recomputation
         * @return the recalibrated tree. It may differ from the original one in structure, score and number of vertices
         */
        public FTree getCorrectedTree(FragmentationPatternAnalysis analyzer);

        /**
         * Recompute the tree and return the corrected tree
         *
         * @param analyzer the analyzer pipeline that should be used for recomputation
         * @param oldTree  (optional) the oldTree given as template for faster computation
         * @return the recalibrated tree. It may differ from the original one in structure, score and number of vertices
         */
        public FTree getCorrectedTree(FragmentationPatternAnalysis analyzer, FTree oldTree);

        /**
         * @return a function which maps each peak mass to its recalibrated mass
         */
        public UnivariateFunction recalibrationFunction();
    }

}
