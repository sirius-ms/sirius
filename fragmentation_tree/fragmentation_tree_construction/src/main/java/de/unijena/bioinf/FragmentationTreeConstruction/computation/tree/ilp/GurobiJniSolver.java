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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TimeoutException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gurobi.*;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * New gurobi solver using the java JNI only
 * NOTES:
 * - gurobi jni uses sparse matrices. Therefore, we set coefs + vars, to say: tuple (coef, var-index).
 *   Remember that when reading the code!
 * Created by Xentrics on 13.11.2014.
 */
public class GurobiJniSolver implements TreeBuilder {
    @Override
    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return null;
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return null;
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

//
//    //////////////////////////////////////
//    ///                                ///
//    ///   TREEBUILDER IMPLEMENTATION   ///
//    ///                                ///
//    //////////////////////////////////////
//
//    private static GRBModel STATIC_MODEL;
//
//    public GurobiJniSolver() {
//        if (STATIC_MODEL==null) try {
//            STATIC_MODEL = new GRBModel(new GRBEnv());
//        } catch (GRBException e) {
//            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
//        }
//    }
//
//    @Override
//    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound) {
//        return null;
//    }
//
//    @Override
//    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
//        return buildTree(input, graph, lowerbound);
//    }
//
//    @Override
//    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
//        try {
//            return new Solver(graph, input, lowerbound, null, 0).solve();
//        } catch (GRBException e) {
//            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
//            return null;
//        }
//    }
//
//    @Override
//    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
//        return null;
//    }
//
//    @Override
//    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound) {
//        return null;
//    }
//
//    @Override
//    public String getDescription() {
//        return "Gurobi JNI";
//    }
//
//
//    ////////////////////////////////
//    ///                          ///
//    ///   SOLVER IMPLEMENATION   ///
//    ///                          ///
//    ////////////////////////////////
//
//
//    private class Solver extends AbstractSolver {
//
//        final protected TreeBuilder feasibleSolver;
//
//        protected long model;
//        protected final long env;
//
//
//
//        public Solver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit) throws GRBException {
//            super(graph, input, lowerbound, feasibleSolver, timeLimit);
//
//            this.env = getDefaultEnv(null);
//
//            int[] error = new int[1];
//            this.model = GurobiJni.newmodel(error, this.env, null, 0, null, null, null, null, null);
//            if (error.length > 0 && error[0] != 0)
//                throw new GRBException("Number of Errors: (" + error.length + "). First error entry: \n" +  GurobiJni.geterrormsg(env), error[0]);
//
//            // set time limit
//            GurobiJniAccess.set( this.env, GRB.DoubleParam.TimeLimit, this.LP_TIMELIMIT);
//
//            this.feasibleSolver = feasibleSolver;
//            setNumberOfCPUs(Runtime.getRuntime().availableProcessors());
//        }
//
//
//
//        /**
//         * - Returns the relative reference as long of the newly create environment
//         * - That value will be needed for the Gurobi Jni Interface
//         * @param logfilename
//         * @return: Long representing the GRBEnv address within the relative area
//         */
//        public long getDefaultEnv(String logfilename) {
//
//            try {
//                long[] jenv = new long[1]; // the new address will be written in here
//                int error = GurobiJni.loadenv(jenv, logfilename);
//                if (error != 0) throw new GRBException(GurobiJni.geterrormsg(jenv[0]), error);
//
//                GurobiJniAccess.set(jenv[0], GRB.IntParam.OutputFlag, 0); // prevent gurobi from writing too much output
//                return jenv[0];
//            } catch (GRBException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//
//
//        //////////////////////////////
//    ///////--- INSTANCE METHODS ---///////
//        //////////////////////////////
//
//
//        @Override
//        public void prepareSolver() {
//            super.prepareSolver();
//
//            try {
//                assert GurobiJniAccess.get(this.model, GRB.IntAttr.IsMIP) != 0;
//                built = true;
//            } catch (GRBException e) {
//                throw new RuntimeException(String.valueOf(e.getErrorCode()), e);
//            }
//        }
//
//
//        @Override
//        /**
//         * This will add all necessary vars through the gurobi jni interface to be used later.
//         *
//         * A variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}.
//         * In our case, every edge will be used only once (at most)
//         * OPTIMIZED I
//         */
//        protected void defineVariablesWithStartValues(FTree presolvedTree) throws GRBException {
//
//            if (this.model == 0L) throw new GRBException("Model not loaded", 20003);
//
//            // First: Acquire start values
//
//            final TObjectIntHashMap<MolecularFormula> map = new TObjectIntHashMap<MolecularFormula>(presolvedTree.numberOfVertices() * 3, 0.5f, -1);
//            int k = 0;
//            for (Fragment f : presolvedTree.getFragments())
//                map.put(f.getFormula(), k++);
//
//            final boolean[][] matrix = new boolean[k][k];
//            for (Fragment f : presolvedTree.getFragmentsWithoutRoot())
//                matrix[map.get(f.getFormula())][map.get(f.getIncomingEdge().getSource().getFormula())] = true;
//
//            final double[] startValues = new double[this.LP_NUM_OF_VARIABLES];
//            for (int i = 0; i < losses.size(); ++i) {
//
//                final Loss l = losses.get(i);
//                final int u = map.get(l.getSource().getFormula());
//
//                if (u < 0)
//                    continue;
//
//                final int v = map.get(l.getTarget().getFormula());
//                if (v < 0)
//                    continue;
//
//                if (matrix[v][u]) startValues[i] = 1.0d;
//            }
//
//            // Second: set up all other attributes
//
//            final double[] ubs = new double[this.LP_NUM_OF_VARIABLES];
//            final double[] lbs = null; // this will cause lbs to be 0.0 by default
//            final double[] obj = new double[this.LP_NUM_OF_VARIABLES];
//            final char[] vtypes = null; // arguments will be assumed to be continuous
//            String[] names = null; // arguments will have default names.
//
//            for(int i : edgeIds) {
//                ubs[i] = 1.0;
//                obj[i] = -losses.get(i).getWeight();
//            }
//
//            final int error = GurobiJni.addvars(model, LP_NUM_OF_VARIABLES, 0, edgeOffsets, edgeIds, startValues, obj , lbs, ubs, vtypes, names);
//            if (error != 0) throw new GRBException(GurobiJni.geterrormsg(this.env), error);
//        }
//
//
//        @Override
//        protected void applyLowerBounds() throws GRBException {
//            if (LP_LOWERBOUND != 0) GurobiJniAccess.set(this.model, GRB.DoubleParam.Cutoff, LP_LOWERBOUND);
//        }
//
//
//        @Override
//        /**
//         * This will add all necessary vars through the gurobi jni interface to be used later.
//         *
//         * A variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}.
//         * In our case, every edge will be used only once (at most)
//         * OPTIMIZED I
//         */
//        protected void defineVariables() throws GRBException {
//
//            if (this.model == 0L) throw new GRBException("Model not loaded", 20003);
//
//            final int nonZero = 0;
//            final int[] vbeg = new int[this.LP_NUM_OF_VARIABLES];
//            final int[] inds = null;
//            final double[] vals = null;
//            final double[] ubs = new double[this.LP_NUM_OF_VARIABLES];
//            final double[] lbs = new double[this.LP_NUM_OF_VARIABLES]; // this will cause lbs to be 0.0 by default
//            final double[] obj = new double[this.LP_NUM_OF_VARIABLES];
//            final char[] vtypes = new char[this.LP_NUM_OF_VARIABLES];
//            String[] names = null; // arguments will have default names.
//
//            for(int i=0; i < losses.size(); i++) {
//                vbeg[i] = i;
//                lbs[i] = 0d;
//                ubs[i] = 1d;
//                obj[i] = -losses.get(i).getWeight(); // var coefficient
//                vtypes[i] = GRB.INTEGER;
//            }
//
//            //final int error = GurobiJni.addvars(model, 2, 0, new int[]{0,1}, null, null, new double[]{1d, 1d}, new double[]{0d, 0d}, new double[]{1d, 1d}, new char[]{GRB.INTEGER, GRB.INTEGER}, null);
//            final int error = GurobiJni.addvars(model, this.LP_NUM_OF_VARIABLES, nonZero, vbeg, inds, vals, obj, lbs, ubs, vtypes, names);
//            if (error != 0) throw new GRBException(GurobiJni.geterrormsg(this.env), error);
//
//            GurobiJni.updatemodel(this.model); // this is important! we could not define constraints afterwards
//
//        }
//
//
//        @Override
//        /**
//         * for each vertex, take one out-going edge at most
//         * => the sum of all variables as edges going away from v is equal or less 1
//         * OPTIMIZED I
//         * TODO: optimize to oneliner
//         */
//        protected void setTreeConstraint() {
//
//            // a variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}
//            // In our case, many edges will not be present. It is sufficient enough to just store those existing ones
//            // and remember their ids. That way, everything is stored in individual arrays, that are accessible through
//            // 'edgeOffsets' and 'edgeIds'.
//
//            // prepare arrays
//            final double[] coefs = new double[LP_NUM_OF_VARIABLES]; // variable coefficients
//            final double[] rhsc = new double[LP_NUM_OF_VARIABLES]; //right-hand-side-constants. That shall be 1
//            final double[] lhsc = null; // left-hand-side-constants. We usually don't have any of those
//            final char[] signs = new char[LP_NUM_OF_VARIABLES]; // equation sign between left and right hand side
//
//            final Fragment pseudoRoot = graph.getRoot();
//            int lossId = 0;
//            for (int k=1; k<this.LP_NUM_OF_VERTICES; k++) { // do not include the root
//
//                final Fragment f = graph.getFragmentAt(k);
//                assert f != pseudoRoot : "Tree constraint must not iterate over root vertex!";
//
//                // define sparse matrix
//                int[] inds = new int[f.getInDegree()+1];
//                double[] vals = new double [f.getInDegree()+1];
//                for (int in = 0; in < f.getInDegree(); ++in) {
//                    assert losses.get(lossId).getTarget() == f;
//                    inds[in] = lossId;
//                    vals[in] = 1d;
//                    lossId++;
//                }
//
//                final int lastIndex = f.getInDegree();
//                vals[lastIndex] = -1d;
//                final int offset = edgeOffsets[k];
//                for (int out=0; out < f.getOutDegree(); ++out ) {
//                    final int outgoingEdge = edgeIds[offset+out];
//                    assert losses.get(outgoingEdge).getSource()==f;
//                    inds[lastIndex] = outgoingEdge;
//
//                    final int[] cbeg = new int[]{0};
//                    final char[] sense = new char[]{GRB.LESS_EQUAL};
//                    final double[] lhs = new double[]{0d};
//                    final double[] rhs = new double[]{Double.POSITIVE_INFINITY}; // could be 1, I guess
//                    GurobiJni.addconstrs(this.model, 1, f.getInDegree()+1, cbeg, inds, vals, sense, lhs, rhs, null); //TODO: this can be wrapped up into one execution. Should be checked for validity first, though!
//                }
//            }
//        }
//
//
//        @Override
//        /**
//         * for each color, take only one incoming edge
//         * the sum of all edges going into color relative is equal or less than 1
//         * TODO: optimize
//         */
//        protected void setColorConstraint() {
//
//            final int COLOR_NUM = this.graph.maxColor()+1;
//
//            // get all edges of each color first. We can add each color-constraint afterwards + save much memory!
//            final TIntArrayList[] edgesOfColors = new TIntArrayList[COLOR_NUM];
//            for (int c=0; c<COLOR_NUM; c++)
//                edgesOfColors[c] = new TIntArrayList();
//
//            int colnum = 0;
//            int l=0;
//            for (int k=0; k < graph.numberOfVertices(); ++k) {
//                final Fragment u = graph.getFragmentAt(k);
//                final TIntArrayList list = edgesOfColors[u.getColor()];
//                for (int i=0; i < u.getInDegree(); ++i) {
//                    list.add(l++);
//                }
//            }
//
//            for (TIntArrayList list : edgesOfColors)
//                if (list.size()>0) ++colnum;
//
//            final int NUM_OF_CONSTRAINTS = COLOR_NUM;
//
//            // add constraints. Maximum one edge per color.
//            int constrIndex=0;
//            for (int c=0; c < edgesOfColors.length; c++) {
//                final TIntArrayList COL_ENTRIES = edgesOfColors[c];
//                if (COL_ENTRIES.isEmpty())
//                    continue;
//
//                final int[] inds = new int[COL_ENTRIES.size()];
//                final double[] coefs = new double[COL_ENTRIES.size()];
//                for (int i=0; i < COL_ENTRIES.size(); i++) {
//                    inds[i] = COL_ENTRIES.get(i);
//                    coefs[i] = 1d;
//                }
//
//                final int[] cbeg = new int[]{0};
//                final char[] sign = new char[]{GRB.LESS_EQUAL};
//                final double[] lhs = new double[]{0d};
//                final double[] rhs = new double[]{1d};
//                GurobiJni.addconstrs(this.model, 1, inds.length, cbeg, inds, coefs, sign, lhs, rhs, null);
//            }
//        }
//
//
//        @Override
//        /**
//         * there should be at least one edge leading away from the root
//         */
//        protected void setMinimalTreeSizeConstraint() {
//
//            final Fragment localRoot = graph.getRoot();
//            final int fromIndex = edgeOffsets[localRoot.getVertexId()];
//            final int toIndex = fromIndex + localRoot.getOutDegree();
//
//            final int[] inds = new int[localRoot.getOutDegree()];
//            final double[] coefs = new double[localRoot.getOutDegree()];
//
//            assert fromIndex <= toIndex;
//            for (int i=fromIndex; i<toIndex; i++) {
//                inds[i-fromIndex] = i; // add variable indices
//                coefs[i-fromIndex] = 1.0d;
//            }
//
//            assert (coefs[toIndex-1] != 0) : "The Last value shouldn't be zero?!";
//
//            final int[] cbeg = new int[]{0};
//            final char[] sign = new char[]{GRB.LESS_EQUAL};
//            final double[] lhs = new double[]{0d};
//            final double[] rhs = new double[]{1d};
//            GurobiJni.addconstrs(model, 1, inds.length, cbeg, inds, coefs, sign, lhs, rhs, null);
//        }
//
//        @Override
//        protected void setObjective() throws Exception {
//            // nothing to do here
//        }
//
//
//        @Override
//        protected SolverState solveMIP() throws Exception {
//            GurobiJni.updatemodel(this.model);
//
//
//            int error;
//
//            final Method opt = GRBModel.class.getDeclaredMethod("jnioptimize", long.class, int.class, int.class);
//            opt.setAccessible(true);
//            error = ((Integer)opt.invoke(STATIC_MODEL, model, 0, 0)).intValue();
//
//            if(error != 0) throw new GRBException(GurobiJni.geterrormsg(this.env), error);
//
//            // TODO: find a hack to use jnioptimize()!
//
//            if (GurobiJniAccess.get(this.model, GRB.IntAttr.Status) != GRB.OPTIMAL) {
//                if (GurobiJniAccess.get(this.model, GRB.IntAttr.Status) == GRB.INFEASIBLE) {
//                    return SolverState.SHALL_RETURN_NULL; // LP_LOWERBOUND reached
//                } else {
//                    errorHandling();
//                    return SolverState.SHALL_RETURN_NULL;
//                }
//            }
//
//            return SolverState.SHALL_BUILD_SOLUTION;
//        }
//
//
//        @Override
//        protected SolverState pastBuildSolution() {
//            GurobiJni.freemodel(this.model); // free memory
//            return SolverState.FINISHED;
//        }
//
//
//        protected void errorHandling() throws Exception {
//            // infeasible solution!
//            int status = GurobiJniAccess.get(this.model, GRB.IntAttr.Status);
//            String cause = "";
//            switch (status) {
//                case GRB.TIME_LIMIT:
//                    cause = "Timeout (exceed time limit of " + this.LP_TIMELIMIT + " seconds per decomposition";
//                    throw new TimeoutException(cause);
//                case GRB.INFEASIBLE:
//                    cause = "Solution is infeasible.";
//                    break;
//                case GRB.CUTOFF:
//                    return;
//                default:
//                    try {
//                        if (GurobiJniAccess.get(this.model, GRB.DoubleAttr.ConstrVioSum) > 0)
//                            cause = "Constraint are violated. Tree-correctness: "
//                                    + isComputationCorrect(buildSolution(), graph, getSolverScore());
//                        else cause = "Unknown error. Status code is " + status;
//                    } catch (GRBException e) {
//                        throw new RuntimeException("Unknown error. Status code is " + status, e);
//                    }
//            }
//
//            throw new RuntimeException("Can't find a feasible solution: " + cause);
//        }
//
//        /**
//         * we need to retrieve the edges being kept in the optimal solution
//         * if an edge has a value greater 0.5, we keep it
//         * @return
//         * @throws GRBException
//         */
//        protected boolean[] getVariableAssignment() throws GRBException {
//
//            final double[] edgesAreUsed = new double[edgeIds.length];
//            final int error = GurobiJni.getdblattrlist(model, "X", edgeIds.length, 0, edgeIds, edgesAreUsed);
//            if (error != 0)
//                throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);
//
//            final boolean[] assignments = new boolean[this.LP_NUM_OF_VARIABLES];
//            //final double tolerance = GurobiJniAccess.get(this.model, GRB.DoubleAttr.IntVio);
//            for (int i = 0; i < assignments.length; ++i) {
//                assert edgesAreUsed[i] > -0.5 : "LP_LOWERBOUND violation for var " + i + " with value " + edgesAreUsed[i];
//                assert edgesAreUsed[i] < 1.5 : "LP_LOWERBOUND violation for var " + i + " with value " + edgesAreUsed[i];
//                assignments[i] = edgesAreUsed[i] > 0.5d;
//            }
//
//            return assignments;
//        }
//
//        @Override
//        protected double getSolverScore() throws Exception {
//
//            return -GurobiJniAccess.get(this.model, GRB.DoubleAttr.ObjVal);
//        }
//
//        public void setNumberOfCPUs(int numberOfCPUs) {
//            if (numberOfCPUs != this.numberOfCPUs) {
//                try {
//                    GurobiJniAccess.set( this.env, GRB.IntParam.Threads, numberOfCPUs);
//                } catch (GRBException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            this.numberOfCPUs = numberOfCPUs;
//
//        }
//    }

}