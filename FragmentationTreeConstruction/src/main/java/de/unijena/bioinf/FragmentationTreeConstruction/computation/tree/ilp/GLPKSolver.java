package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.glp_prob;

/**
 * NOTES:
 * - GLPK is not thread safe
 * Created by xentrics on 04.03.15.
 */
public class GLPKSolver extends AbstractSolver {

    final static String GLPK_VERSION = "4_55";

    final glp_prob LP;

    protected int NumOfVariables;
    protected int NumOfVertices;

    protected int[] vars;


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

        // loadGLPKlibrary()
        this.LP = GLPK.glp_create_prob();
        GLPK.glp_set_prob_name(this.LP, "ColSubtreeProbGLPK");
        System.out.println("GLPK: Problem created");
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
            //constraints: GLPK.glp_set_col_bnd(LP, i, ...)
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

    }

    @Override
    protected void setColorConstraint() throws Exception {

    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {

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
