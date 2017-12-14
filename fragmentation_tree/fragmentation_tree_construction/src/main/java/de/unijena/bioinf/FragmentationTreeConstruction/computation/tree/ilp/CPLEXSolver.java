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

package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;

import java.util.Arrays;

/**
 * Created by kaidu on 05.01.16.
 */
public class CPLEXSolver extends AbstractSolver {

    protected IloCplex model;
    protected IloIntVar[] variables;
    protected IloLPMatrix constraints;

    public CPLEXSolver(FGraph graph) {
        super(graph);
    }

    public CPLEXSolver(FGraph graph, double lowerbound) {
        super(graph, lowerbound);
    }

    protected CPLEXSolver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit) {
        super(graph, input, lowerbound, feasibleSolver, timeLimit);
    }

    @Override
    public void prepareSolver() {

        try {
            model = new IloCplex();
            model.setOut(null);
            model.setParam(IloCplex.IntParam.Threads, 1);
            constraints = model.addLPMatrix();
        } catch (IloException e) {
            throw new RuntimeException(e);
        }

        super.prepareSolver();
    }

    @Override
    protected void defineVariables() throws Exception {
        variables = model.boolVarArray(losses.size());
        constraints.addCols(variables);

    }

    @Override
    protected void defineVariablesWithStartValues(FTree presolvedTree) throws Exception {

    }

    @Override
    protected void applyLowerBounds() throws Exception {
        if (LP_LOWERBOUND>0) {
            final double[] optim = new double[losses.size()];
            final int[] indizes = new int[losses.size()];
            for (int i=0; i < optim.length; ++i) {
                optim[i] = losses.get(i).getWeight();
                indizes[i] = i;
            }
            constraints.addRow(LP_LOWERBOUND, Double.POSITIVE_INFINITY, indizes, optim);
        }
    }


    protected void setTreeConstraint() throws IloException {
        int k = 0;
        for (Fragment fragment : graph.getFragmentsWithoutRoot()) {
            final int[] indizesLeft = new int[fragment.getInDegree()];
            final double[] onesLeft = new double[fragment.getInDegree()];
            Arrays.fill(onesLeft, 1d);
            for (int l = 0; l < fragment.getInDegree(); ++l) {
                indizesLeft[l] = k++;
            }
            // 1. For all vertices u with u!=root the sum of all edges uv for a fixed v is <= 1
            // => TreeCondition: Each vertex (except root has a parent
            constraints.addRow(0, 1, indizesLeft, onesLeft);
            //model.addConstr(expression, GRB.LESS_EQUAL, 1, null);//String.valueOf(identifier++));
            // 2. An edge is only set, if one of the incomming edges of its head is set
            // => Connectivity-Condition: There is a path between each two vertices
            int j = edgeOffsets[fragment.getVertexId()];

            final int[] indizes2 = Arrays.copyOf(indizesLeft, indizesLeft.length+1);
            final double[] row2 = Arrays.copyOf(onesLeft, onesLeft.length+1);
            final int n = indizesLeft.length;
            row2[n] = -1d;
            for (int l = 0; l < fragment.getOutDegree(); ++l) {
                indizes2[n] = edgeIds[j];
                constraints.addRow(0, 1, indizes2, row2);
                assert losses.get(edgeIds[j]).getSource() == fragment;
                ++j;
            }
        }
    }

    @Override
    protected void setColorConstraint() throws IloException {

        final int[] colorSizes = new int[graph.maxColor()+1];
        for (Loss l : losses) {
            ++colorSizes[l.getTarget().getColor()];
        }
        final int[][] indizesPerColor = new int[colorSizes.length][];
        for (int c=0; c < colorSizes.length; ++c) {
            if (colorSizes[c]>0)
                indizesPerColor[c] = new int[colorSizes[c]];
        }

        int k = 0;
        for (Loss l : losses) {
            final int C = l.getTarget().getColor();
            indizesPerColor[C][--colorSizes[C]] = k;
            ++k;
        }
        for (int i = 0; i < indizesPerColor.length; ++i) {
            if (indizesPerColor[i]!=null) {
                final double[] ones = new double[indizesPerColor[i].length];
                Arrays.fill(ones, 1d);
                constraints.addRow(0, 1, indizesPerColor[i], ones);//String.valueOf(++identifier));
            }
        }
    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {

        final int[] subroots = new int[graph.getRoot().getOutDegree()];
        final double[] ones = new double[subroots.length];
        Arrays.fill(ones, 1d);
        int from = edgeOffsets[graph.getRoot().getVertexId()];
        int k=0;
        for (int i=from, n = from+subroots.length; i < n; ++i) {
            subroots[k++] = edgeIds[i];
        }
        constraints.addRow(1, Double.POSITIVE_INFINITY, subroots, ones);

    }

    @Override
    protected void setObjective() throws Exception {
        final double[] weights = new double[losses.size()];
        for (int i=0; i < weights.length; ++i) {
            weights[i] = losses.get(i).getWeight();
        }
        model.addObjective(IloObjectiveSense.Maximize, model.scalProd(variables, weights));
    }

    @Override
    protected SolverState solveMIP() throws Exception {
        model.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Auto);
        model.setParam(IloCplex.IntParam.NodeAlg, IloCplex.Algorithm.Auto);
        final boolean done = model.solve();
        if (done) return SolverState.SHALL_BUILD_SOLUTION;
        else return SolverState.SHALL_RETURN_NULL;
    }

    @Override
    protected SolverState pastBuildSolution() throws Exception {
        model.endModel();
        model.end();
        return SolverState.FINISHED;
    }

    @Override
    protected boolean[] getVariableAssignment() throws Exception {
        final double[] weights = model.getValues(variables);
        final boolean[] assigned = new boolean[weights.length];
        for (int i=0; i < weights.length; ++i) {
            assigned[i] = weights[i] > 0.5d;
        }
        return assigned;
    }

    @Override
    protected double getSolverScore() throws Exception {
        return model.getObjValue();
    }
}
