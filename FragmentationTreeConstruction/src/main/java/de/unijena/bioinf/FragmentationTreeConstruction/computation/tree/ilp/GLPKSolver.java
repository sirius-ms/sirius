package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.SWIGTYPE_p_double;

/**
 * NOTES:
 * - GLPK indices start with 1, not zero !!!
 * - GLPK is not thread safe
 * - GLPK uses an index array of type 'SWIGTYPE_p_int' to set up constraints
 * Created by xentrics on 04.03.15.
 */
public class GLPKSolver extends AbstractSolver {

    final static String GLPK_VERSION = "4_55";

    final glp_prob LP;

    final SWIGTYPE_p_int GLP_INDEX_ARRAY;


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
        GLPK.glp_set_obj_dir(this.LP, GLPKConstants.GLP_MAX);
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


    /**
     * try to load the jni interface for glpk from the glpk library
     */
    static {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // try to load Windows library
            try {
                System.loadLibrary("glpk_" + GLPKSolver.GLPK_VERSION);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Could not load glpk_4.55 library from windows! Make sure to have the correct" +
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


    ///////////////////////
    ///     METHODS     ///
    ///////////////////////


    @Override
    protected void defineVariables() throws Exception {
        final int N = this.losses.size();
        GLPK.glp_add_cols(this.LP, N);

        for (int i=1; i<=N; i++) {
            GLPK.glp_set_col_name(LP, i, "loss"+i);
            GLPK.glp_set_col_kind(LP, i, GLPKConstants.GLP_CV); //TODO: check
            GLPK.glp_set_col_bnds(LP, i, GLPKConstants.GLP_DB, 0.0d, 1.0d); // either zero or one!
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
        for (int k=1; k<=this.LP_NUM_OF_VERTICES; k++) {

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
        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, this.LP_NUM_OF_VERTICES);
    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {

        // returns the index of the first newly created row
        final int CONSTR_START_INDEX = GLPK.glp_add_rows(this.LP, this.LP_NUM_OF_VERTICES);
    }

    @Override
    protected void setObjective() throws Exception {
        // nothing to do yet? TODO: check
    }

    @Override
    protected int preBuildSolution() throws Exception {
        return 0;
    }

    @Override
    protected int pastBuildSolution() throws Exception {
        return 0;
    }

    @Override
    protected FTree buildSolution() throws Exception {
        return null;
    }
}
