/*
*
* This file is part of the SIRIUS library for analyzing MS and MS/MS data
*
* Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
* Chair of Bioinformatics, Friedrich-Schilller University.

* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>
*/
package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.sirius.ProcessedInput;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.gnu.glpk.*;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GLPKSolver extends AbstractSolver {

    protected final static Lock GLPK_LOCK = new ReentrantLock();
    protected glp_prob LP;
    protected glp_iocp parm;

    public final static IlpFactory<GLPKSolver> Factory = new IlpFactory<>() {
        @Override
        public GLPKSolver create(ProcessedInput input, FGraph graph, TreeBuilder.FluentInterface options) {
            return new GLPKSolver(graph, input, options);
        }

        @Override
        public boolean isThreadSafe() {
            return false;
        }

        @Override
        public String name() {
            return "GLPK";
        }

        @Override
        public void checkSolver() throws ILPSolverException {
            try {
                GLPK.glp_create_prob();
            } catch (Throwable e) {
                throw new ILPSolverException(e);
            }
        }
    };

    protected GLPKSolver(FGraph graph, ProcessedInput input, TreeBuilder.FluentInterface options) {
        super(graph, input, options);
    }

    @Override
    public TreeBuilder.Result compute() {
        GLPK_LOCK.lock();
        try {
            return super.compute();
        } finally {
            GLPK_LOCK.unlock();
        }
    }

    @Override
    protected void setTimeLimitInSeconds(double timeLimitsInSeconds) throws Exception {
        parm.setTm_lim((int)(timeLimitsInSeconds*1000));
    }

    @Override
    protected void setNumberOfCpus(int numberOfCPUS) throws Exception {
        // not supported
        if (numberOfCPUS!=1) {
            LoggerFactory.getLogger(GLPKSolver.class).warn("GLPK does not support multitreading.");
        }
    }

    @Override
    protected void initializeModel() throws Exception {
        this.LP = GLPK.glp_create_prob();
        GLPK.glp_set_prob_name(this.LP, "ColSubtreeProbGLPK");
        GLPK.glp_java_set_msg_lvl(GLPKConstants.GLP_JAVA_MSG_LVL_OFF);
        GLPK.glp_term_out(GLPKConstants.GLP_OFF);
        parm = new glp_iocp();
        GLPK.glp_init_iocp(parm);
        parm.setPresolve(GLPKConstants.GLP_ON);
    }

    @Override
    protected void setMinimalScoreConstraints(double minimalScore) throws Exception {

    }

    @Override
    protected void defineVariables() throws Exception {
        final int N = losses.size();
        GLPK.glp_add_cols(this.LP, N);
        // for GLPK: structure variables
        for (int i=1; i<=N; i++) {
            //GLPK.glp_set_col_name(LP, i, null);
            GLPK.glp_set_col_kind(LP, i, GLPKConstants.GLP_BV); //Integer Variable
        }
    }

    @Override
    protected void setVariableStartValues(int[] usedEdgeIds) throws Exception {
        // not supported
    }

    @Override
    protected void setTreeConstraint() throws Exception {
// returns the index of the first newly created row
        final int N = graph.numberOfEdges()-graph.getRoot().getOutDegree();
        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, N);

        // create auxiliary variables first
        for (int r=CONSTR_START_INDEX; r < CONSTR_START_INDEX + N; r++) {
            //GLPK.glp_set_row_name(this.LP, r, "r"+r);
            GLPK.glp_set_row_bnds(this.LP, r, GLPKConstants.GLP_LO, 0.0, 0.0);
        }

        int CONSTR_INDEX = CONSTR_START_INDEX;
        final Fragment pseudoRoot = graph.getRoot();
        // set up row entries
        int lossId = 0;
        for (int k=0; k<graph.numberOfVertices(); k++) {
            final Fragment f = graph.getFragmentAt(k);
            if (f==pseudoRoot) continue;
            {
                SWIGTYPE_p_int rowIndizes = GLPK.new_intArray(f.getInDegree() + 2);
                SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(f.getInDegree() + 2);
                int rowIndex = 1;
                for (int in=0; in < f.getInDegree(); ++in) {
                    assert losses.get(lossId).getTarget() == f;
                    GLPK.intArray_setitem(rowIndizes, rowIndex, ++lossId); // each edge is weighted equally here
                    GLPK.doubleArray_setitem(rowValues, rowIndex, 1d);
                    ++rowIndex;
                }

                GLPK.doubleArray_setitem(rowValues, rowIndex, -1d);
                final int offset = edgeOffsets[k];
                for (int out=0; out < f.getOutDegree(); ++out ) {
                    final int outgoingEdge = edgeIds[offset+out];
                    assert losses.get(outgoingEdge).getSource()==f;
                    GLPK.intArray_setitem(rowIndizes, rowIndex, outgoingEdge + 1); // each edge is weighted equally here
                    GLPK.glp_set_mat_row(this.LP, CONSTR_INDEX, f.getInDegree()+1, rowIndizes, rowValues);
                    assert CONSTR_INDEX < CONSTR_START_INDEX+N;
                    ++CONSTR_INDEX;
                }
                GLPK.delete_intArray(rowIndizes); // free memory. Doesn't delete lp matrix entry!
                GLPK.delete_doubleArray(rowValues); // free memory. Doesn't delete lp matrix entry!
            }
        }
        assert CONSTR_INDEX==CONSTR_START_INDEX+N;
    }

    @Override
    protected void setColorConstraint() throws Exception {
        final int COLOR_NUM = this.graph.maxColor() + 1;
        final TIntArrayList[] edgesOfColors = new TIntArrayList[COLOR_NUM];
        for (int c = 0; c < COLOR_NUM; c++)
            edgesOfColors[c] = new TIntArrayList();

        int colnum = 0;
        int l = 0;
        for (int k = 0; k < graph.numberOfVertices(); ++k) {
            final Fragment u = graph.getFragmentAt(k);
            final TIntArrayList list = edgesOfColors[u.getColor()];
            for (int i = 0; i < u.getInDegree(); ++i) {
                list.add(l++);
            }
        }

        for (TIntArrayList list : edgesOfColors)
            if (list.size() > 0) ++colnum;

        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, colnum);
        for (int c = CONSTR_START_INDEX; c < CONSTR_START_INDEX + colnum; ++c) {
            GLPK.glp_set_row_bnds(this.LP, c, GLPKConstants.GLP_UP, 0.0, 1.0);
        }

        // add constraints. Maximum one edge per color.
        int constrIndex = 0;
        for (int c = 0; c < edgesOfColors.length; c++) {
            final TIntArrayList COL_ENTRIES = edgesOfColors[c];
            if (COL_ENTRIES.isEmpty()) continue;
            SWIGTYPE_p_int rowIndizes = GLPK.new_intArray(COL_ENTRIES.size() + 1); // alloc memory
            SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(COL_ENTRIES.size() + 1); // alloc memory
            for (int i = 1; i <= COL_ENTRIES.size(); i++) {
                GLPK.intArray_setitem(rowIndizes, i, COL_ENTRIES.get(i - 1) + 1);
                GLPK.doubleArray_setitem(rowValues, i, 1.0d);
            }
            GLPK.glp_set_mat_row(this.LP, CONSTR_START_INDEX + constrIndex++, COL_ENTRIES.size(), rowIndizes, rowValues);
            GLPK.delete_intArray(rowIndizes); // free memory
            GLPK.delete_doubleArray(rowValues); // free memory

        }
    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {
        // returns the index of the first newly created row
        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, 1);
        GLPK.glp_set_row_name(this.LP, CONSTR_START_INDEX, null);
        GLPK.glp_set_row_bnds(this.LP, CONSTR_START_INDEX, GLPKConstants.GLP_FX, 1.0, 1.0); // fixed variable!

        final Fragment localRoot = graph.getRoot();
        final int fromIndex = edgeOffsets[localRoot.getVertexId()];
        final int toIndex = fromIndex + localRoot.getOutDegree();

        SWIGTYPE_p_int rowIndizes = GLPK.new_intArray(localRoot.getOutDegree()+1);
        SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(localRoot.getOutDegree()+1);
        int i=1;
        for (int k=fromIndex; k<toIndex; k++) {
            GLPK.doubleArray_setitem(rowValues, i, 1.0);
            GLPK.intArray_setitem(rowIndizes, i, k+1);
            ++i;
        }


        GLPK.glp_set_mat_row(this.LP, CONSTR_START_INDEX, localRoot.getOutDegree(), rowIndizes, rowValues);
        GLPK.delete_intArray(rowIndizes); // free memory
        GLPK.delete_doubleArray(rowValues); // free memory
    }

    @Override
    protected void setObjective() throws Exception {
        // nothing to do yet?
        GLPK.glp_set_obj_dir(this.LP, GLPKConstants.GLP_MAX);
        GLPK.glp_set_obj_name(this.LP, "z");

        GLPK.glp_set_obj_coef(this.LP, 0, 0.0); // non-variables constant of function

        final int N = losses.size();
        int k=1;
        for (int i=0; i < graph.numberOfVertices(); ++i) {
            final Fragment u = graph.getFragmentAt(i);
            for (int j=0; j < u.getInDegree(); ++j) {
                GLPK.glp_set_obj_coef(this.LP, k++, u.getIncomingEdge(j).getWeight());
            }
        }
    }

    @Override
    protected TreeBuilder.AbortReason solveMIP() throws Exception {

        int result = GLPK.glp_intopt(this.LP, parm);



        int status = GLPK.glp_mip_status(this.LP);
        if (status == GLPKConstants.GLP_OPT) {
        } else if (status == GLPKConstants.GLP_FEAS) {
            LoggerFactory.getLogger(this.getClass()).info("The solution is feasible.");
        } else if (status == GLPKConstants.GLP_INFEAS) {
            LoggerFactory.getLogger(this.getClass()).info("The solution is infeasible!");
            return TreeBuilder.AbortReason.INFEASIBLE;
        } else if (status == GLPKConstants.GLP_NOFEAS) {
            LoggerFactory.getLogger(this.getClass()).info("The problem has no feasible solution!");
            return TreeBuilder.AbortReason.NO_SOLUTION;
        } else if (status == GLPKConstants.GLP_UNBND) {
            LoggerFactory.getLogger(this.getClass()).info("The problem has unbound solution!");
        } else if (status == GLPKConstants.GLP_UNDEF) {
            LoggerFactory.getLogger(this.getClass()).info("The solution is undefined!");
        }

        if (result == 0)
            return TreeBuilder.AbortReason.COMPUTATION_CORRECT;
        else if (result == GLPKConstants.GLP_EBADB) {
            LoggerFactory.getLogger(this.getClass()).error("Unable to start the search, because the initial basis speci\fed in the problem object\n" +
                    "is invalid|the number of basic (auxiliary and structural) variables is not the same\n" +
                    "as the number of rows in the problem object.");
            throw new InvalidPropertiesFormatException("GLPKSolver algorithm is not correctly set up!");
        } else if (result == GLPKConstants.GLP_ESING) {
            LoggerFactory.getLogger(this.getClass()).error("Unable to start the search, because the basis matrix corresponding to the initial\n" +
                    "basis is exactly singular.");
            throw new InvalidPropertiesFormatException("GLPKSolver algorithm is not correctly set up!");
        } else if (result == GLPKConstants.GLP_EBOUND) {
            LoggerFactory.getLogger(this.getClass()).error("Unable to start the search, because some double-bounded (auxiliary or structural)\n" +
                    "variables have incorrect bounds.");
            throw new InvalidPropertiesFormatException("GLPKSolver algorithm is not correctly set up!");
        } else if (result == GLPKConstants.GLP_EFAIL) {
            LoggerFactory.getLogger(this.getClass()).error("The problem does not have variables or conditions!");
        } else if (result == GLPKConstants.GLP_EITLIM) {
            LoggerFactory.getLogger(this.getClass()).error("GLPK reached iteration limit! Prematurely termination.");
            return TreeBuilder.AbortReason.TIMEOUT;
        } else if (result == GLPKConstants.GLP_ETMLIM) {
            return TreeBuilder.AbortReason.TIMEOUT;
        } else {
            LoggerFactory.getLogger(this.getClass()).error("Unrecognized return value from simplex. Abort!");
        }
        return TreeBuilder.AbortReason.INFEASIBLE;
    }

    @Override
    protected void pastBuildSolution() throws Exception {
        parm.delete();
        GLPK.glp_delete_prob(this.LP); // free memory
    }

    @Override
    protected boolean[] getVariableAssignment() throws Exception {

        final boolean[] assignments = new boolean[this.losses.size()];
            final int N = GLPK.glp_get_num_cols(this.LP);
            for (int x=1; x<=N; x++) {
                final double VAL = GLPK.glp_mip_col_val(this.LP, x);
                assert VAL > -0.5 : "LP_LOWERBOUND violation for var " + x + " with value " + VAL;
                assert VAL < 1.5  : "LP_LOWERBOUND violation for var " + x + " with value " + VAL;
                assignments[x-1] = VAL > 0.5d;
            }
        {
            final ArrayList<Loss> losses = new ArrayList<Loss>();
            int k=0;
            for (int i=0; i < graph.numberOfVertices(); ++i) {
                final Fragment u = graph.getFragmentAt(i);
                for (int j=0; j < u.getInDegree(); ++j) {
                    final Loss l = u.getIncomingEdge(j);
                    if (assignments[k++]) losses.add(l);
                }
            }
            final TIntSet colorset = new TIntHashSet();
            boolean colorful = true;
            for (Loss l : losses) {
                final int color = l.getTarget().getColor();
                if (colorset.contains(color)) {
                    colorful=false;
                } else colorset.add(color);
            }
            assert colorful;
        }

        return assignments;
    }

    @Override
    protected double getSolverScore() throws Exception {
        return GLPK.glp_mip_obj_val(this.LP);
    }
}
