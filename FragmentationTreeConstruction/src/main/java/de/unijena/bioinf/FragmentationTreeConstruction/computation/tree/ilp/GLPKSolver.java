package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeScoring;
import gnu.trove.list.array.TIntArrayList;
import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.SWIGTYPE_p_double;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

/**
 * NOTES:
 * - GLPK indices start with 1, not zero !!!
 * - GLPK is not thread safe
 * - GLPK uses an index array of type 'SWIGTYPE_p_int' to set up constraints
 * - GLPKConstants.IV: Integer Variable, x € Int
 * - GLPKConstants.CV: Continuous Variable, x € R
 * - GLPKConstants.DB: Double bound, lb < x < ub
 * TODO: TreeConstraint: is integer problem?
 * TODO: ColorConstraint: is integer problem?
 * TODO: timlimit?
 * TODO: global lower bound?
 * Created by xentrics on 04.03.15.
 */
public class GLPKSolver extends AbstractSolver {

    final static String GLPK_VERSION = "4_55";

    final glp_prob LP;

    final SWIGTYPE_p_int GLP_INDEX_ARRAY;

    /*********************************************************************
     * try to load the jni interface for glpk from the glpk library      *
     * will be loaded the moment an instance of 'GLPKSolver' is created  *
     *********************************************************************/
    static {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // try to load Windows library
            try {
                System.loadLibrary("glpk_" + GLPKSolver.GLPK_VERSION);
                System.out.println("Using system glpk \nVersion: " + GLPK.glp_version());
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Could not load glpk_4.55 library from windows! Make sure to have the correct" +
                        " version of glpk installed on your system!");
                throw e;
            }
        } else {
            try {
                System.loadLibrary("glpk_java");
                System.out.println("Using java glpk \nVersion: " + GLPK.glp_version());
            } catch (UnsatisfiedLinkError e) {
                System.err.println("The dynamic link library for GLPK for java could not be loaded. \n" +
                        "Consider using \njava -Djava.library.path=");
                throw e;
            }
        }
    }

    ///////////////////////////
    ///     CONSTRUCTORS    ///
    ///////////////////////////

    public GLPKSolver(FGraph graph) {
        this(graph, null, Double.NEGATIVE_INFINITY, null, -1);
    }

    public GLPKSolver(FGraph graph, double lowerbound) {
        this(graph, null, lowerbound, null, -1);
    }

    protected GLPKSolver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit) {
        super(graph, input, lowerbound, feasibleSolver, timeLimit);

        System.out.printf("Using: GLPK solver...");
        // loadGLPKlibrary()
        this.LP = GLPK.glp_create_prob();
        GLPK.glp_set_prob_name(this.LP, "ColSubtreeProbGLPK");
        System.out.println("GLPK: Problem created...");

        this.GLP_INDEX_ARRAY = prepareIndexArray();
        System.out.println("Matrix prepared...");
    }


    final SWIGTYPE_p_int prepareIndexArray() {

        final SWIGTYPE_p_int INDEX_ARR = GLPK.new_intArray(LP_NUM_OF_VARIABLES + 1); // + 1 for the right-hand-side column

        for (int i=1; i<=LP_NUM_OF_VARIABLES; i++)
            GLPK.intArray_setitem(INDEX_ARR, i, i);

        return INDEX_ARR;
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
            GLPK.glp_set_col_name(LP, i, "loss"+i);
            GLPK.glp_set_col_kind(LP, i, GLPKConstants.GLP_IV); //Integer Variable
            GLPK.glp_set_col_bnds(LP, i, GLPKConstants.GLP_DB, 0.0, 1.0); // double-bound | either zero or one!
        }
    }

    @Override
    protected void defineVariablesWithStartValues(FTree presolvedTree) throws Exception {
        throw new UnsupportedOperationException("Not implmented yet!");
    }

    @Override
    protected void applyLowerBounds() throws Exception {

    }

    @Override
    protected void setTreeConstraint() throws Exception {

        // returns the index of the first newly created row
        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, this.LP_NUM_OF_VERTICES);

        // create auxiliary variables first
        for (int k=1; k<=this.LP_NUM_OF_VERTICES; k++) {
            GLPK.glp_set_row_name(this.LP, k, "r"+k);
            GLPK.glp_set_row_bnds(this.LP, k, GLPKConstants.GLP_IV, 0.0, 1.0); // right-hand-side | maximum one edge!
        }

        // set up row entries
        for (int k=0; k<this.LP_NUM_OF_VERTICES; k++) {

            SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(this.LP_NUM_OF_VARIABLES + 1);
            final int LAST_INDEX = (k == this.LP_NUM_OF_VARIABLES -1) ? LP_NUM_OF_VARIABLES -1 : edgeOffsets[k+1] -1;

            for (int i= edgeOffsets[k]+1; i<=LAST_INDEX; i++)
                GLPK.doubleArray_setitem(rowValues, i, 1.0); // each edge is weighted equally here

            GLPK.glp_set_mat_row(this.LP, CONSTR_START_INDEX + k, this.LP_NUM_OF_VARIABLES + 1, this.GLP_INDEX_ARRAY, rowValues);

            // TODO: check - other entries zero by default?
            GLPK.delete_doubleArray(rowValues); // free memory. Doesn't delete lp matrix entry!
        }
    }

    @Override
    protected void setColorConstraint() throws Exception {

        // returns the index of the first newly created row
        final int COLOR_NUM = this.graph.maxColor()+1;
        final boolean[] colorInUse = new boolean[COLOR_NUM];

        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, COLOR_NUM);
        for (int c=1; c<=COLOR_NUM; c++) {
            GLPK.glp_set_row_name(this.LP, c, "c" + c);
            GLPK.glp_set_row_bnds(this.LP, c, GLPKConstants.GLP_IV, 0.0, 1.0);
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
        for (int c=0; c<COLOR_NUM; c++) {
            SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(this.LP_NUM_OF_VARIABLES + 1); // alloc memory
            final TIntArrayList COL_ENTRIES = edgesOfColors[c];
            for (int i=0; i<COL_ENTRIES.size(); i++)
                GLPK.doubleArray_setitem(rowValues, COL_ENTRIES.get(i), 1.0);

            GLPK.glp_set_mat_row(this.LP, CONSTR_START_INDEX + c, this.LP_NUM_OF_VARIABLES + 1, this.GLP_INDEX_ARRAY, rowValues);

            GLPK.delete_doubleArray(rowValues); // free memory
        }
    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {
        System.out.println("GLPK: SetMinimalTreeSize...");

        // returns the index of the first newly created row
        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, 1);
        GLPK.glp_set_row_name(this.LP, CONSTR_START_INDEX, "MinTree");
        GLPK.glp_set_row_bnds(this.LP, CONSTR_START_INDEX, GLPKConstants.GLP_IV, 0.0, 1.0);

        final Fragment localRoot = graph.getRoot();
        final int fromIndex = edgeOffsets[localRoot.getVertexId()];
        final int toIndex = fromIndex + localRoot.getOutDegree();

        SWIGTYPE_p_double rowValues = GLPK.new_doubleArray(this.LP_NUM_OF_VARIABLES + 1);
        for (int k=fromIndex; k<toIndex; k++)
            GLPK.doubleArray_setitem(rowValues, k, 1.0);

        GLPK.glp_set_mat_row(this.LP, CONSTR_START_INDEX, this.LP_NUM_OF_VARIABLES+1, this.GLP_INDEX_ARRAY, rowValues);
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
        for (Loss l : losses ) {
            GLPK.glp_set_obj_coef(this.LP, k, l.getWeight());
            k++;
        }

        assert (k == this.LP_NUM_OF_VARIABLES) : "the objective function should contain the same amount of coefs as the number of variables just!";
    }

    @Override
    protected int preBuildSolution() throws Exception {

        glp_smcp parm = new glp_smcp();
        GLPK.glp_init_smcp(parm);

        int result = GLPK.glp_simplex(this.LP, parm);

        int status = GLPK.glp_get_status(this.LP);
        if (status == GLPKConstants.GLP_OPT) {
            System.out.println("The solution is optimal.");
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
            return AbstractSolver.SHALL_BUILD_SOLUTION;
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
            return AbstractSolver.SHALL_RETURN_NULL;
        } else if (result == GLPKConstants.GLP_EITLIM) {
            System.err.println("GLPK reached iteration limit! Prematurely termination.");
            return AbstractSolver.SHALL_RETURN_NULL;
        } else if (result == GLPKConstants.GLP_ETMLIM) {
            System.err.println("GLPK reached time limit! Prematurely termination.");
            return AbstractSolver.SHALL_RETURN_NULL;
        } else {
            System.err.println("Unrecognized return value from simplex. Abort!");
            return AbstractSolver.SHALL_RETURN_NULL;
        }
    }

    @Override
    protected int pastBuildSolution() throws Exception {
        GLPK.glp_delete_prob(this.LP); // free memory
        System.out.println("GLPK solver finished.");
        return AbstractSolver.FINISHED;
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
            assignments[x] = VAL > 0.5d;
        }

        return assignments;
    }

    @Override
    protected double getSolverScore() throws Exception {
        return GLPK.glp_get_obj_val(this.LP);
    }
}
