package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import gnu.trove.map.hash.TObjectIntHashMap;
import gurobi.*;

/**
 * Created by Spectar on 13.11.2014.
 */
public class NewGurobiSolver extends AbstractSolver {

    //TODO: Timelimit?

    protected final long envNative;
    final protected TreeBuilder feasibleSolver;

    protected final long model;
    protected final GRBVar[] variables;


    public NewGurobiSolver(FGraph graph, ProcessedInput input, double lowerbound, long env, TreeBuilder feasibleSolver, int timeLimit) throws GRBException {
        super(graph, input, lowerbound, feasibleSolver, timeLimit);

        this.envNative = env;

        int[] error = new int[1];
        this.model = GurobiJni.newmodel(error, this.envNative, null, 0, null, null, null, null, null);
        this.variables = new GRBVar[this.losses.size()];
        GurobiJniAccess.set( this.model, GRB.DoubleParam.TimeLimit, 0);

        this.feasibleSolver = feasibleSolver;
    }


    /**
     * - Returns the c reference as long of the newly create environment
     * - That value will be needed for the Gurobi Jni Interface
     * @param logfilename
     * @return: Long representing the GRBEnv address within the c area
     */
    public static long getDefaultEnv(String logfilename) {

        try {

            long[] jenv = new long[1]; // the new address will be written in here
            int error = GurobiJni.loadenv(jenv, logfilename);
            if (error != 0) throw new GRBException(GurobiJni.geterrormsg(jenv[0]), error);

            // env.set(GRB.IntParam.OutputFlag, 0);
            return jenv[0];
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void build() {

        super.build();
        try {
            assert GurobiJniAccess.get(this.model, GRB.IntAttr.IsMIP) != 0;
            built = true;
        } catch (GRBException e) {
            throw new RuntimeException(String.valueOf(e.getErrorCode()), e);
        }
    }


    @Override
    protected void defineVariables() throws GRBException {

        if (this.model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        }

        double[] valarr = new double[losses.size()];
        double[] lbs = new double[losses.size()];
        double[] ubs = new double[losses.size()];
        int[] beg = new int[losses.size()];
        int[] ind = null;
        char[] types = new char[1]; // this may have to be bigger
        String[] names = new String[1]; // this may have to to be bigger

        for(int i=0; i < losses.size(); i++) {
            //lbs[i] = 0.0; // redundant
            ubs[i] = 1.0;
            valarr[i] = -losses.get(i).getWeight();
        }

        int error = GurobiJni.addvars(this.model, losses.size(), 0, beg, ind, null, valarr, lbs, ubs, types, names);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(this.model)), error);
        }
        //update model?
    }


    @Override
    protected void setStartValues(FTree presolvedTree) throws GRBException {

        final TObjectIntHashMap<MolecularFormula> map = new TObjectIntHashMap<MolecularFormula>(presolvedTree.numberOfVertices() * 3, 0.5f, -1);
        int k = 0;
        for (Fragment f : presolvedTree.getFragments())
            map.put(f.getFormula(), k++);

        final boolean[][] matrix = new boolean[k][k];
        for (Fragment f : presolvedTree.getFragmentsWithoutRoot())
            matrix[map.get(f.getFormula())][map.get(f.getIncomingEdge().getSource().getFormula())] = true;

        final double[] startValues = new double[losses.size()];
        for (int i = 0; i < losses.size(); ++i) {

            final Loss l = losses.get(i);
            final int u = map.get(l.getSource().getFormula());

            if (u < 0)
                continue;

            final int v = map.get(l.getTarget().getFormula());
            if (v < 0)
                continue;

            if (matrix[v][u]) startValues[i] = 1.0d;
        }

        // TODO: Get that setter method implemented (not easy though)
        // GurobiJniAccess.set(GRB.DoubleAttr.Start, variables, startValues);
    }


    @Override
    protected void computeOffsets() {

        for (int k = 1; k < offsets.length; ++k)
            offsets[k] = offsets[k - 1] + graph.getFragmentAt(k - 1).getOutDegree();

        // why do that here?
        for (int k = 0; k < losses.size(); ++k) {
            final int u = losses.get(k).getSource().getVertexId();
            edgeIds[offsets[u]++] = k;
        }

        // this seems to be needed because of the questioned code above
        for (int k = 0; k < offsets.length; ++k)
            offsets[k] -= graph.getFragmentAt(k).getOutDegree();
    }


    @Override
    protected void setLowerbound() throws GRBException {
        if (lowerbound != 0) {
            GurobiJniAccess.set(this.model, GRB.DoubleParam.Cutoff, 0.0);
        }
    }


    @Override
    protected void setTreeConstraint() {

    }


    @Override
    protected void setColorConstraint() {

    }


    @Override
    protected void setMinimalTreeSizeConstraint() {

    }


    @Override
    protected FTree solve() {
        return null;
    }


    //-- THIS SECTION MAY BE PACKED INTO A SEPERATE PACKAGE --//
        //-- IT WILL ALLOW USING THE JNI INTERFACE DIRECTLY --//



}
