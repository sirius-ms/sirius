package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import gnu.trove.list.array.TIntArrayList;
import org.gnu.glpk.*;

import java.util.InvalidPropertiesFormatException;
import java.util.List;

/**
 * NOTES:
 * - GLPK arrays have Num_Of_Variables + 1 entries, because the first entry of each array starts at index 1!
 * - GLPK is not thread safe
 * - GLPK uses sparse matrices ( and does therefore always needs an array of indices and index of values )
 * - GLPK uses an index array of type 'SWIGTYPE_p_int'
 * - GLPK uses a value array of type 'SWIGTYPE_p_double'
 * - GLPKConstants.IV: Integer Variable, x € Z
 * - GLPKConstants.CV: Continuous Variable, x € R
 * - GLPKConstants.DB: Double bound, lb < x < ub
 * - GLPKConstants.LO: Only Lower Bound, lb < x < infinity
 * - GLPKConstraints.FX: fixed variables a <= x <= a
 * TODO: TreeConstraint: the second part should contain the first condition, too!?
 * TODO: timlimit?
 * TODO: global lower bound?
 * Created by xentrics on 04.03.15.
 */
public class GLPKSolver implements TreeBuilder {

    /*********************************************************************
     * try to load the jni interface for glpk from the glpk library      *
     * will be loaded the moment an instance of 'GLPKSolver' is created  *
     *********************************************************************/
    static {
        final String versionString = GLPK.GLP_MAJOR_VERSION + "_" + GLPK.GLP_MINOR_VERSION;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // try to load Windows library
            try {
                System.loadLibrary("glpk_" + versionString);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Could not load glpk library from windows! Make sure to have the correct" +
                        " version of glpk installed on your system!");
                throw e;
            }
        } else {
            try {
                System.loadLibrary("glpk_java");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("The dynamic link library for GLPK for java could not be loaded. \n" +
                        "Consider using \njava -Djava.library.path=");
                throw e;
            }
        }
    }


    //////////////////////////////////////
    ///                                ///
    ///   TREEBUILDER IMPLEMENTATION   ///
    ///                                ///
    //////////////////////////////////////


    @Override
    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return buildTree(input, graph, lowerbound);
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
        return new Solver(graph, input, lowerbound, null, 0).solve();
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return null;
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }


    /////////////////////////////////
    ///                           ///
    ///   SOLVER IMPLEMENTATION   ///
    ///                           ///
    /////////////////////////////////


    /**
     * The actual solver. Instances will be initiated from outer class
     */
    private class Solver extends AbstractSolver {

        final glp_prob LP;

        public Solver(FGraph graph) {
            this(graph, null, Double.NEGATIVE_INFINITY, null, -1);
        }

        public Solver(FGraph graph, double lowerbound) {
            this(graph, null, lowerbound, null, -1);
        }

        protected Solver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit) {
            super(graph, input, lowerbound, feasibleSolver, timeLimit);
            this.LP = GLPK.glp_create_prob();
            GLPK.glp_set_prob_name(this.LP, "ColSubtreeProbGLPK");
        }


        ///////////////////////
        ///     METHODS     ///
        ///////////////////////


        @Override
        protected void defineVariables() throws Exception {
            final int N = this.losses.size();
            GLPK.glp_add_cols(this.LP, N);

            // for GLPK: structure variables
            for (int i=1; i<=N; i++) {
                GLPK.glp_set_col_name(LP, i, null);
                GLPK.glp_set_col_kind(LP, i, GLPKConstants.GLP_BV); //Integer Variable
                //GLPK.glp_set_col_bnds(LP, i, GLPKConstants.GLP_DB, 0.0, 1.0); // double-bound | either zero or one!
            }
        }

        @Override
        protected void defineVariablesWithStartValues(FTree presolvedTree) throws Exception {
            throw new UnsupportedOperationException("Not implmented yet!");
        }

        @Override
        protected void applyLowerBounds() throws Exception {
            if (true) return;

            // TODO: check increase of speed
        /*
        // the sum of all edges kept in the solution should be higher than the given lower bound
        if (LP_LOWERBOUND != 0) {
            final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, this.LP_NUM_OF_VARIABLES);
            GLPK.glp_set_row_name(this.LP, CONSTR_START_INDEX, "low_bnd");
            GLPK.glp_set_row_bnds(this.LP, CONSTR_START_INDEX, GLPKConstants.GLP_LO, this.LP_LOWERBOUND, Double.POSITIVE_INFINITY);
            SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(this.LP_NUM_OF_VARIABLES + 1);

            int k = 1;
            for (Loss l : this.losses) {
                GLPK.doubleArray_setitem(rowValues, k, l.getWeight());
                k++;
            }

            GLPK.glp_set_mat_row(this.LP, CONSTR_START_INDEX, this.LP_NUM_OF_VARIABLES + 1, this.GLP_INDEX_ARRAY, rowValues);
        }
        */
        }


        @Override
        protected void setTreeConstraint() {

            // returns the index of the first newly created row
            final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, this.LP_NUM_OF_VERTICES + this.LP_NUM_OF_VARIABLES - graph.getRoot().getOutDegree());

            // create auxiliary variables first
            for (int r=CONSTR_START_INDEX; r < CONSTR_START_INDEX + this.LP_NUM_OF_VERTICES + this.LP_NUM_OF_VARIABLES - graph.getRoot().getOutDegree(); r++) {
                GLPK.glp_set_row_bnds(this.LP, r, GLPKConstants.GLP_DB, 0.0, 1.0); // right-hand-side | maximum one edge!
            }

            int CONSTR_INDEX = CONSTR_START_INDEX;

            // set up row entries
            int lossId = 0;
            for (int k=1; k<this.LP_NUM_OF_VERTICES; k++) {

                final Fragment f = graph.getFragmentAt(k);
                assert !f.isRoot() : "The root must not be present to set up tree constraint";

                {
                    SWIGTYPE_p_int rowIndizes = GLPK.new_intArray(f.getInDegree()+1);
                    SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(f.getInDegree()+1);
                    int l=lossId;
                    int colIndex = 1;
                    for (Loss loss : f.getIncomingEdges()) {
                        assert losses.get(l).getTarget().equals(f);
                        GLPK.intArray_setitem(rowIndizes, colIndex, ++l); // each edge is weighted equally here
                        GLPK.doubleArray_setitem(rowValues, colIndex, 1d);
                        ++colIndex;
                    }
                    GLPK.glp_set_mat_row(this.LP, CONSTR_INDEX, f.getInDegree(), rowIndizes, rowValues);
                    ++CONSTR_INDEX;
                    GLPK.delete_intArray(rowIndizes); // free memory. Doesn't delete lp matrix entry!
                    GLPK.delete_doubleArray(rowValues); // free memory. Doesn't delete lp matrix entry!
                }

                {

                    SWIGTYPE_p_int rowIndizes = GLPK.new_intArray(f.getInDegree() + 2);
                    SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(f.getInDegree() + 2);
                    int colIndex = 1;
                    int l = lossId;
                    for (Loss loss : f.getIncomingEdges()) {
                        GLPK.intArray_setitem(rowIndizes, colIndex, ++l); // each edge is weighted equally here
                        GLPK.doubleArray_setitem(rowValues, colIndex, 1d);
                        ++colIndex;
                    }

                    final int fromIndex = edgeOffsets[f.getVertexId()];
                    final int toIndex = fromIndex + f.getOutDegree();
                    for (int i=fromIndex; i < toIndex; i++) {
                        assert colIndex <= (f.getInDegree()+1);
                        GLPK.intArray_setitem(rowIndizes, colIndex, edgeIds[i] + 1); // each edge is weighted equally here
                        GLPK.doubleArray_setitem(rowValues, colIndex, -1d);
                        GLPK.glp_set_mat_row(this.LP, CONSTR_INDEX, f.getInDegree()+1, rowIndizes, rowValues);
                        ++CONSTR_INDEX;
                    }

                    GLPK.delete_intArray(rowIndizes); // free memory. Doesn't delete lp matrix entry!
                    GLPK.delete_doubleArray(rowValues); // free memory. Doesn't delete lp matrix entry!
                    lossId = l;
                }
            }

            /*
                THIS SHOULD ALREADY SUFFICE!
                DO NOT DELETE THIS UNTIL INVALIDATED

            // returns the index of the first newly created row
            final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, this.LP_NUM_OF_VARIABLES - graph.getRoot().getOutDegree());

            // create auxiliary variables first
            for (int r=CONSTR_START_INDEX; r < CONSTR_START_INDEX + this.LP_NUM_OF_VARIABLES - graph.getRoot().getOutDegree(); r++) {
                GLPK.glp_set_row_bnds(this.LP, r, GLPKConstants.GLP_DB, 0.0, 1.0); // right-hand-side | maximum one edge!
            }

            int CONSTR_INDEX = CONSTR_START_INDEX;

            // set up row entries
            int lossId = 0;
            for (int k=1; k<this.LP_NUM_OF_VERTICES; k++) {

                final Fragment f = graph.getFragmentAt(k);
                assert !f.isRoot();

                SWIGTYPE_p_int rowIndizes = GLPK.new_intArray(f.getInDegree() + 2);
                SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(f.getInDegree() + 2);
                int colIndex = 1;
                int l = lossId;
                for (Loss loss : f.getIncomingEdges()) {
                    GLPK.intArray_setitem(rowIndizes, colIndex, ++l); // each edge is weighted equally here
                    GLPK.doubleArray_setitem(rowValues, colIndex, 1d);
                    ++colIndex;
                }

                final int fromIndex = edgeOffsets[f.getVertexId()];
                final int toIndex = fromIndex + f.getOutDegree();
                for (int i=fromIndex; i < toIndex; i++) {
                    assert colIndex <= (f.getInDegree()+1);
                    GLPK.intArray_setitem(rowIndizes, colIndex, edgeIds[i] + 1); // each edge is weighted equally here
                    GLPK.doubleArray_setitem(rowValues, colIndex, -1d);
                    GLPK.glp_set_mat_row(this.LP, CONSTR_INDEX, f.getInDegree()+1, rowIndizes, rowValues);
                    ++CONSTR_INDEX;
                }

                GLPK.delete_intArray(rowIndizes); // free memory. Doesn't delete lp matrix entry!
                GLPK.delete_doubleArray(rowValues); // free memory. Doesn't delete lp matrix entry!
                lossId = l;
            }
            */
        }


        @Override
        protected void setColorConstraint() {
            // returns the index of the first newly created row
            final int COLOR_NUM = this.graph.maxColor()+1;
            final boolean[] colorInUse = new boolean[COLOR_NUM];

            final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, COLOR_NUM);
            for (int c=CONSTR_START_INDEX; c < CONSTR_START_INDEX + COLOR_NUM; c++) {
                GLPK.glp_set_row_name(this.LP, c, null);
                GLPK.glp_set_row_bnds(this.LP, c, GLPKConstants.GLP_DB, 0.0, 1.0);
            }

            // get all edges of each color first. We can add each color-constraint afterwards + saving much memory!
            final TIntArrayList[] edgesOfColors = new TIntArrayList[COLOR_NUM];
            for (int c=0; c<COLOR_NUM; c++)
                edgesOfColors[c] = new TIntArrayList();

            int k=0;
            for (Loss l : this.losses) {
                final int c = l.getTarget().getColor();
                edgesOfColors[c].add(k);
                k++;
            }

            // add constraints. Maximum one edge per color.
            for (int c=0; c < COLOR_NUM; c++) {
                final TIntArrayList COL_ENTRIES = edgesOfColors[c];
                SWIGTYPE_p_int rowIndizes = GLPK.new_intArray(COL_ENTRIES.size()+1); // alloc memory
                SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(COL_ENTRIES.size()+1); // alloc memory
                for (int i=1; i<=COL_ENTRIES.size(); i++) {
                    GLPK.intArray_setitem(rowIndizes, i, COL_ENTRIES.get(i-1)+1);
                    GLPK.doubleArray_setitem(rowValues, i, 1.0d);
                }
                GLPK.glp_set_mat_row(this.LP, CONSTR_START_INDEX + c, COL_ENTRIES.size(), rowIndizes, rowValues);
                GLPK.delete_intArray(rowIndizes); // free memory
                GLPK.delete_doubleArray(rowValues); // free memory

            }
        }


        @Override
        protected void setMinimalTreeSizeConstraint() {

            // returns the index of the first newly created row
            final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, 1);
            GLPK.glp_set_row_name(this.LP, CONSTR_START_INDEX, null);
            GLPK.glp_set_row_bnds(this.LP, CONSTR_START_INDEX, GLPKConstants.GLP_FX, 1.0, 1.0); // fixed variable! TODO: check!

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
        protected void setObjective()  {
            // nothing to do yet?
            GLPK.glp_set_obj_dir(this.LP, GLPKConstants.GLP_MAX);
            GLPK.glp_set_obj_name(this.LP, "z");

            GLPK.glp_set_obj_coef(this.LP, 0, 0.0); // non-variables constant of function

            final int N = losses.size();
            int k=1;
            for (Loss l : losses ) {
                GLPK.glp_set_obj_coef(this.LP, k, l.getWeight());
                k++;
            }

            assert ((k-1) == this.LP_NUM_OF_VARIABLES) : "the objective function should contain the same amount of coefs as the number of variables just! (" + k  + " vs. " + LP_NUM_OF_VARIABLES;
        }


        @Override
        protected SolverState solveMIP() throws InvalidPropertiesFormatException {

            glp_smcp parm = new glp_smcp();
            GLPK.glp_init_smcp(parm);

            int result = GLPK.glp_simplex(this.LP, parm);

            try {
            } catch (Exception e) {
                e.printStackTrace();
            }

            int status = GLPK.glp_get_status(this.LP);
            if (status == GLPKConstants.GLP_OPT) {
            } else if (status == GLPKConstants.GLP_FEAS) {
                System.out.println("The solution is feasible.");
            } else if (status == GLPKConstants.GLP_INFEAS) {
                System.out.println("The solution is infeasible!");
            } else if (status == GLPKConstants.GLP_NOFEAS) {
                System.out.println("The problem has no feasible solution!");
            } else if (status == GLPKConstants.GLP_UNBND) {
                System.out.println("The problem has unbound solution!");
            } else if (status == GLPKConstants.GLP_UNDEF) {
                System.out.println("The solution is undefined!");
            }

            if (result == 0)
                return SolverState.SHALL_BUILD_SOLUTION;
            else if (result == GLPKConstants.GLP_EBADB) {
                System.err.println("Unable to start the search, because the initial basis speci\fed in the problem object\n" +
                        "is invalid|the number of basic (auxiliary and structural) variables is not the same\n" +
                        "as the number of rows in the problem object.");
                throw new InvalidPropertiesFormatException("GLPKSolver algorithm is not correctly set up!");
            } else if (result == GLPKConstants.GLP_ESING) {
                System.err.println("Unable to start the search, because the basis matrix corresponding to the initial\n" +
                        "basis is exactly singular.");
                throw new InvalidPropertiesFormatException("GLPKSolver algorithm is not correctly set up!");
            } else if (result == GLPKConstants.GLP_EBOUND) {
                System.err.println("Unable to start the search, because some double-bounded (auxiliary or structural)\n" +
                        "variables have incorrect bounds.");
                throw new InvalidPropertiesFormatException("GLPKSolver algorithm is not correctly set up!");
            } else if (result == GLPKConstants.GLP_EFAIL) {
                System.out.println("The problem does not have variables or conditions!");
                return SolverState.SHALL_RETURN_NULL;
            } else if (result == GLPKConstants.GLP_EITLIM) {
                System.err.println("GLPK reached iteration limit! Prematurely termination.");
                return SolverState.SHALL_RETURN_NULL;
            } else if (result == GLPKConstants.GLP_ETMLIM) {
                System.err.println("GLPK reached time limit! Prematurely termination.");
                return SolverState.SHALL_RETURN_NULL;
            } else {
                System.err.println("Unrecognized return value from simplex. Abort!");
                return SolverState.SHALL_RETURN_NULL;
            }
        }


        @Override
        protected SolverState pastBuildSolution() throws Exception {
            GLPK.glp_delete_prob(this.LP); // free memory
            return SolverState.FINISHED;
        }


        /**
         * we need to retrieve the edges being kept in the optimal solution
         * if an edge has a value greater 0.5, we keep it
         * @return
         */
        final protected boolean[] getVariableAssignment() {
            final boolean[] assignments = new boolean[this.LP_NUM_OF_VARIABLES];

            final int N = GLPK.glp_get_num_cols(this.LP);
            for (int x=1; x<=N; x++) {
                final double VAL = GLPK.glp_get_col_prim(this.LP, x);
                assert VAL > -0.5 : "LP_LOWERBOUND violation for var " + x + " with value " + VAL;
                assert VAL < 1.5  : "LP_LOWERBOUND violation for var " + x + " with value " + VAL;
                assignments[x-1] = VAL > 0.5d;
            }

            return assignments;
        }


        @Override
        protected double getSolverScore() throws Exception {
            return GLPK.glp_get_obj_val(this.LP);
        }
    }

}