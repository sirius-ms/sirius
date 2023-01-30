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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLPSolver extends AbstractSolver {

    public final static IlpFactory<CLPSolver> Factory = new IlpFactory<>() {
        @Override
        public CLPSolver create(ProcessedInput input, FGraph graph, TreeBuilder.FluentInterface options) {
            return new CLPSolver(graph, input, options);
        }

        @Override
        public boolean isThreadSafe() {
            return false; // TODO: check
        }

        @Override
        public String name() {
            return "COIN-OR LP";
        }

        @Override
        public void checkSolver() throws ILPSolverException {
            try {
                new CLPModel_JNI(0,0);
            } catch (Throwable e) {
                LoggerFactory.getLogger(getClass()).error("Error loading CBC!", e);
                throw new ILPSolverException(e);
            }
        }
    };

    protected CLPModel_JNI model;

    public CLPSolver(FGraph graph, ProcessedInput input, TreeBuilder.FluentInterface options) {
        super(graph, input, options);
    }

    @Override
    protected void setTimeLimitInSeconds(double timeLimitsInSeconds) throws Exception {
        this.model.setTimeLimit(timeLimitsInSeconds);
    }

    @Override
    protected void setNumberOfCpus(int numberOfCPUS) throws Exception {
        // not supported
        if (numberOfCPUS!=1)
            LoggerFactory.getLogger(CLPSolver.class).warn("CLP does not support multitreading.");
    }

    @Override
    protected void initializeModel() throws Exception {
        this.model = new CLPModel_JNI(losses.size(), CLPModel_JNI.ObjectiveSense.MAXIMIZE);
    }

    @Override
    protected void setMinimalScoreConstraints(double minimalScore) throws Exception {
        // TODO: is this possible?
    }

    @Override
    protected void defineVariables() throws Exception {
        final double[] lb = new double[losses.size()];
        final double[] ub = new double[losses.size()];
        final double[] objective = new double[losses.size()];
        for (int i = 0; i < losses.size(); ++i) {
            lb[i] = 0d;
            ub[i] = 1d;
            objective[i] = losses.get(i).getWeight();
        }
        model.setColBounds(lb, ub);
        model.setObjective(objective);
    }

    @Override
    protected void setVariableStartValues(int[] usedEdgeIds) throws Exception {
        final double[] values = new double[losses.size()];
        for (int index : usedEdgeIds)
            values[index] = 1d;
        model.setColStart(values);
    }

    @Override
    protected void setTreeConstraint() throws Exception {
        int k = 0;
        for (Fragment fragment : graph.getFragmentsWithoutRoot()) {
            final int[] indices = new int[fragment.getInDegree()];
            final double[] elems = new double[fragment.getInDegree()];
            Arrays.fill(elems, 1d);
            for (int l = 0; l < fragment.getInDegree(); ++l)
                indices[l] = k++;
            model.addSparseRowCached(elems, indices, 0d, 1d);
            // 1. For all vertices u with u!=root the sum of all edges uv for a fixed v is
            // <= 1
            // => TreeCondition: Each vertex (except root has a parent
            // 2. An edge is only set, if one of the incomming edges of its head is set
            // => Connectivity-Condition: There is a path between each two vertices
            int j = edgeOffsets[fragment.getVertexId()];
            final int[] indices2 = Arrays.copyOf(indices, indices.length + 1);
            final double[] elems2 = Arrays.copyOf(elems, elems.length + 1);
            final int n = indices.length;
            elems2[n] = -1d;
            for (int l = 0; l < fragment.getOutDegree(); ++l) {
                indices2[n] = edgeIds[j];
                model.addSparseRowCached(elems2, indices2, 0d, 1d);
                assert losses.get(edgeIds[j]).getSource() == fragment;
                ++j;
            }
        }
    }

    @Override
    protected void setColorConstraint() throws Exception {
        final int[] colorSizes = new int[graph.maxColor() + 1];
        for (Loss l : losses) {
            ++colorSizes[l.getTarget().getColor()];
        }
        final int[][] indizesPerColor = new int[colorSizes.length][];
        for (int c = 0; c < colorSizes.length; ++c) {
            if (colorSizes[c] > 0)
                indizesPerColor[c] = new int[colorSizes[c]];
        }

        int k = 0;
        for (Loss l : losses) {
            final int C = l.getTarget().getColor();
            indizesPerColor[C][--colorSizes[C]] = k;
            ++k;
        }
        for (int i = 0; i < indizesPerColor.length; ++i) {
            if (indizesPerColor[i] != null) {
                final double[] ones = new double[indizesPerColor[i].length];
                Arrays.fill(ones, 1d);
                model.addSparseRowCached(ones, indizesPerColor[i], 0d, 1d);
            }
        }
    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {
        final int[] subroots = new int[graph.getRoot().getOutDegree()];
        final double[] ones = new double[subroots.length];
        Arrays.fill(ones, 1d);
        int from = edgeOffsets[graph.getRoot().getVertexId()];
        int k = 0;
        for (int i = from, n = from + subroots.length; i < n; ++i) {
            subroots[k++] = edgeIds[i];
        }
        model.addSparseRowCached(ones, subroots, 1d, model.getInfinity());
    }

    @Override
    protected void setObjective() throws Exception {
        // already done
    }

    protected static final Logger logger = LoggerFactory.getLogger(CLPSolver.class);

    @Override
    protected TreeBuilder.AbortReason solveMIP() throws Exception {
        int return_status = model.solve();
        // TODO: how to handle timeouts, score cutoff
        switch (return_status) {
        case CLPModel_JNI.ReturnStatus.OPTIMAL:
            return TreeBuilder.AbortReason.COMPUTATION_CORRECT;
        case CLPModel_JNI.ReturnStatus.INFEASIBLE:
            logger.info("Solution is infeasible");
            return TreeBuilder.AbortReason.INFEASIBLE;
        case CLPModel_JNI.ReturnStatus.ABANDONED:
            logger.info("Model was abandoned");
        case CLPModel_JNI.ReturnStatus.LIMIT_REACHED:
            logger.info("Objective and/or iteration limits were reached");
        default:
            return TreeBuilder.AbortReason.NO_SOLUTION;
        }
    }

    @Override
    protected void pastBuildSolution() throws Exception {
        model.dispose();
    }

    @Override
    protected boolean[] getVariableAssignment() throws Exception {
        final double[] weights = model.getColSolution();
        final boolean[] assigned = new boolean[weights.length];
        for (int i = 0; i < weights.length; ++i) {
            assigned[i] = weights[i] > 0.5d;
        }
        return assigned;
    }

    @Override
    protected double getSolverScore() throws Exception {
        return model.getScore();
    }
}
