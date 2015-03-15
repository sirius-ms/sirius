package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TimeoutException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeScoring;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gurobi.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Spectar on 13.11.2014.
 */
public class NewGurobiSolver extends AbstractSolver {

    final protected TreeBuilder feasibleSolver;

    protected final long model;
    protected final long env;

    protected int NumOfVariables;
    protected int NumOfVertices;

    protected int[] vars;


    public NewGurobiSolver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit) throws GRBException {
        super(graph, input, lowerbound, feasibleSolver, timeLimit);

        this.env = getDefaultEnv(null);

        // TODO: NOTE: THIS IS COPIED FROM GUROBI CLASS. IT DOESN'T MATCH THE C REFERENCE; IF STH. GOES WRONG, CHECK THAT FIRST!
        int[] error = new int[1];
        this.model = GurobiJni.newmodel(error, this.env, null, 0, null, null, null, null, null);
        if (error.length > 0 && error[0] != 0)
            throw new GRBException("Number of Errors: (" + error.length + "). First error entry: \n" +  GurobiJni.geterrormsg(env), error[0]);

        GurobiJniAccess.set( this.env, GRB.DoubleParam.TimeLimit, 0);

        this.feasibleSolver = feasibleSolver;

        NumOfVertices = graph.numberOfVertices();

        losses = graph.losses();
        assert (losses != null);
    }



    ////////////////////////////
///////--- STATIC METHODS ---//////
    ////////////////////////////



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

            GurobiJniAccess.set(jenv[0], GRB.IntParam.OutputFlag, 0); // prevent gurobi from writing too much output
            return jenv[0];
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }



    //////////////////////////////
///////--- INSTANCE METHODS ---///////
    //////////////////////////////



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
    /**
     * This will add all necessary vars through the gurobi jni interface to be used later.
     *
     * A variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}.
     * In our case, every edge will be used only once (at most)
     * OPTIMIZED I
     */
    protected void defineVariablesWithStartValues(FTree presolvedTree) throws GRBException {

        if (this.model == 0L) throw new GRBException("Model not loaded", 20003);

        // First: Acquire start values

        final TObjectIntHashMap<MolecularFormula> map = new TObjectIntHashMap<MolecularFormula>(presolvedTree.numberOfVertices() * 3, 0.5f, -1);
        int k = 0;
        for (Fragment f : presolvedTree.getFragments())
            map.put(f.getFormula(), k++);

        final boolean[][] matrix = new boolean[k][k];
        for (Fragment f : presolvedTree.getFragmentsWithoutRoot())
            matrix[map.get(f.getFormula())][map.get(f.getIncomingEdge().getSource().getFormula())] = true;

        final double[] startValues = new double[this.NumOfVariables];
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

        // Second: set up all other attributes

        NumOfVariables = this.losses.size();
        this.vars = new int[NumOfVariables];

        final double[] ubs = new double[this.NumOfVariables];
        final double[] lbs = null; // this will cause lbs to be 0.0 by default
        final double[] obj = new double[this.NumOfVariables];
        final char[] vtypes = null; // arguments will be assumed to be continuous
        String[] names = null; // arguments will have default names.

        for(int i : edgeIds) {
            ubs[i] = 1.0;
            obj[i] = -losses.get(i).getWeight();
        }

        final int error = GurobiJni.addvars(model, NumOfVariables, 0, offsets, edgeIds, startValues, obj , lbs, ubs, vtypes, names);
        if (error != 0) throw new GRBException(GurobiJni.geterrormsg(this.env), error);
    }


    @Override
    protected void applyLowerBounds() throws GRBException {
        if (lowerbound != 0) GurobiJniAccess.set(this.model, GRB.DoubleParam.Cutoff, 0.0);
    }


    @Override
    /**
     * This will add all necessary vars through the gurobi jni interface to be used later.
     *
     * A variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}.
     * In our case, every edge will be used only once (at most)
     * OPTIMIZED I
     */
    protected void defineVariables() throws GRBException {

        if (this.model == 0L) throw new GRBException("Model not loaded", 20003);

        NumOfVariables = this.losses.size();
        this.vars = new int[NumOfVariables];

        final double[] vals = new double[this.NumOfVariables];
        final double[] ubs = new double[this.NumOfVariables];
        final double[] lbs = null; // this will cause lbs to be 0.0 by default
        final double[] obj = new double[this.NumOfVariables];
        final char[] vtypes = null; // arguments will be assumed to be continuous
        String[] names = null; // arguments will have default names.

        for(int i=0; i < losses.size(); i++) {
            ubs[i] = 1.0;
            obj[i] = -losses.get(i).getWeight();
            vals[i] = 1.0d; // edges
        }

        final int error = GurobiJni.addvars(model, NumOfVariables, 0, offsets, edgeIds, vals, obj , lbs, ubs, vtypes, names);
        if (error != 0) throw new GRBException(GurobiJni.geterrormsg(this.env), error);

    }


    @Override
    /**
     * for each vertex, take one out-going edge at most
     * => the sum of all variables as edges going away from v is equal or less 1
     * OPTIMIZED I
     */
    protected void setTreeConstraint() {

        // a variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}
        // In our case, many edges will not be present. It is sufficient enough to just store those existing ones
        // and remember their ids. That way, everything is stored in individual arrays, that are accessible through
        // 'offsets' and 'edgeIds'.

        // prepare arrays
        final double[] coefs = new double[NumOfVariables]; // variable coefficients
        final double[] rhsc = new double[NumOfVariables]; //right-hand-side-constants. That shall be 1
        final double[] lhsc = null; // left-hand-side-constants. We usually don't have any of those
        final char[] signs = new char[NumOfVariables]; // equation sign between left and right hand side

        for (int k=0; k<this.NumOfVertices; k++) {
            // NumOfVariables is NumOfEdges!
            final int LAST_INDEX = (k == this.NumOfVariables-1) ? NumOfVariables -1 : offsets[k+1] -1;

            for (int i=offsets[k]; i<LAST_INDEX; i++)
                coefs[i] = 1.0; // each edge is weighted equally here

            // we want to only use 1 edge from any vertex at max
            signs[k] = GRB.LESS_EQUAL;
            rhsc[k] = 1.0d;
        }

        GurobiJni.addconstrs(model, offsets.length, coefs.length, offsets, edgeIds, coefs, signs, lhsc, rhsc ,null);
    }


    @Override
    /**
     * for each color, take only one incoming edge
     * the sum of all edges going into color c is equal or less than 1
     * TODO: optimize
     */
    protected void setColorConstraint() {

        final boolean[] colorInUse = new boolean[graph.maxColor()+1];

        /* Concept:
         * - each color has a given amount of incoming edges
         * - we already defined those edges as variables ( 'defineVariables()' )
         * - we already applied the tree constraint
         * - know, we gather all edges of 1 color and make a second constraint, that the maximum
         *   amount of edges used to reach that color is 1 edge
         */

        // prepare arrays
        TDoubleArrayList[] coefs = new TDoubleArrayList[graph.maxColor()+1]; // hey are all 1
        TIntArrayList[] vars = new TIntArrayList[this.NumOfVariables]; // I do not know how many edges are going into on color :/
        double[] constants = new double[]{1.0d}; // they are all 1
        char[] signs = new char[]{GRB.LESS_EQUAL}; // the are all GRB.LESS_EQUAL

        for (int e : this.edgeIds) { // edgeIds are equal to the index of an edge
            final int color = losses.get(e).getTarget().getColor();
            colorInUse[color] = true; // we may skip colors we did not use, later
            coefs[color].add(1.0d); // each edge has an impact on 1
            vars[color].add(e);
        }


        // add our constraints
        for (int c=0; c<graph.maxColor()+1; c++) {
            if (colorInUse[c]) {
                GurobiJni.addconstrs(model, 1, coefs[c].size(), new int[]{0}, vars[c].toArray(), coefs[c].toArray(), signs, constants, null, null);
            }
        }
    }


    @Override
    /**
     * there should be at least one edge leading away from the root
     */
    protected void setMinimalTreeSizeConstraint() {
        final Fragment localRoot = graph.getRoot();
        final int fromIndex = offsets[localRoot.getVertexId()];
        final int toIndex = fromIndex + localRoot.getOutDegree();

        final int[] indices = new int[toIndex-fromIndex+1];
        final double[] coefs = new double[toIndex-fromIndex+1];

        for (int i=fromIndex; i<=toIndex; i++) {
            indices[i-fromIndex] = i; // add variable indices
            coefs[i-fromIndex] = 1.0d;
        }

        assert (indices[toIndex] != 0) : "The Last value shouldn't be zero?!";

        GurobiJni.addconstrs(model, 1, coefs.length, new int[]{fromIndex}, indices, coefs, new char[]{GRB.GREATER_EQUAL}, new double[]{1.0d}, null, null);
    }


    @Override
    protected int preBuildSolution() throws GRBException {
        GurobiJni.tunemodel(this.model); //TODO: check: is this optimizing?

        if (GurobiJniAccess.get(this.model, GRB.IntAttr.Status) != GRB.OPTIMAL) {
            if (GurobiJniAccess.get(this.model, GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                return AbstractSolver.SHALL_RETURN_NULL; // lowerbound reached
            } else {
                errorHandling();
                return AbstractSolver.SHALL_RETURN_NULL;
            }
        }

        return AbstractSolver.DO_NOTHING;
    }


    @Override
    protected int pastBuildSolution() {
        GurobiJni.freemodel(this.model); // free memory
        return AbstractSolver.DO_NOTHING;
    }


    @Override
    protected FTree buildSolution() throws GRBException {
        final double score = -GurobiJniAccess.get(this.model, GRB.DoubleAttr.ObjVal);

        final boolean[] edesAreUsed = getVariableAssignment();
        Fragment graphRoot = null;
        final List<FragmentAnnotation<Object>> fAnos = graph.getFragmentAnnotations();
        final List<LossAnnotation<Object>> lAnos = graph.getLossAnnotations();
        final List<FragmentAnnotation<Object>> fTrees = new ArrayList<FragmentAnnotation<Object>>();
        final List<LossAnnotation<Object>> lTrees = new ArrayList<LossAnnotation<Object>>();
        double rootScore = 0d;
        // get root
        {
            int offset = offsets[graph.getRoot().getVertexId()];
            for (int j = 0; j < graph.getRoot().getOutDegree(); ++j) {
                if (edesAreUsed[edgeIds[offset]]) {
                    final Loss l = losses.get(edgeIds[offset]);
                    graphRoot = l.getTarget();
                    rootScore = l.getWeight();
                    break;
                }
                ++offset;
            }
        }
        assert graphRoot != null;
        if (graphRoot == null) return null;

        final FTree tree = newTree(graph, new FTree(graphRoot.getFormula()), rootScore, rootScore);
        for (FragmentAnnotation<Object> x : fAnos) fTrees.add(tree.addFragmentAnnotation(x.getAnnotationType()));
        for (LossAnnotation<Object> x : lAnos) lTrees.add(tree.addLossAnnotation(x.getAnnotationType()));
        final TreeScoring scoring = tree.getAnnotationOrThrow(TreeScoring.class);
        for (int k = 0; k < fAnos.size(); ++k) fTrees.get(k).set(tree.getRoot(), fAnos.get(k).get(graphRoot));

        final ArrayDeque<Stackitem> stack = new ArrayDeque<Stackitem>();
        stack.push(new Stackitem(tree.getRoot(), graphRoot));
        while (!stack.isEmpty()) {
            final Stackitem item = stack.pop();
            final int u = item.graphNode.getVertexId();
            int offset = offsets[u];
            for (int j = 0; j < item.graphNode.getOutDegree(); ++j) {
                if (edesAreUsed[edgeIds[offset]]) {
                    final Loss l = losses.get(edgeIds[offset]);
                    final Fragment child = tree.addFragment(item.treeNode, l.getTarget().getFormula());
                    for (int k = 0; k < fAnos.size(); ++k)
                        fTrees.get(k).set(child, fAnos.get(k).get(l.getTarget()));
                    for (int k = 0; k < lAnos.size(); ++k)
                        lTrees.get(k).set(child.getIncomingEdge(), lAnos.get(k).get(l));

                    child.getIncomingEdge().setWeight(l.getWeight());
                    stack.push(new Stackitem(child, l.getTarget()));
                    scoring.setOverallScore(scoring.getOverallScore() + l.getWeight());
                }
                ++offset;
            }
        }
        return tree;
    }


    protected void errorHandling() throws GRBException {
        // infeasible solution!
        int status = GurobiJniAccess.get(this.model, GRB.IntAttr.Status);
        String cause = "";
        switch (status) {
            case GRB.TIME_LIMIT:
                cause = "Timeout (exceed time limit of " + this.timelimit + " seconds per decomposition";
                throw new TimeoutException(cause);
            case GRB.INFEASIBLE:
                cause = "Solution is infeasible.";
                break;
            case GRB.CUTOFF:
                return;
            default:
                try {
                    if (GurobiJniAccess.get(this.model, GRB.DoubleAttr.ConstrVioSum) > 0)
                        cause = "Constraint are violated. Tree-correctness: "
                                + isComputationCorrect(buildSolution(), graph);
                    else cause = "Unknown error. Status code is " + status;
                } catch (GRBException e) {
                    throw new RuntimeException("Unknown error. Status code is " + status, e);
                }
        }
        throw new RuntimeException("Can't find a feasible solution: " + cause);
    }

    /**
     * - check, whether or not some edges can be ignored by the given lower bound
     * - return an array of values {0,1}, where 0 is a non-existent edge and 1 does exist
     * @return
     * @throws GRBException
     */
    protected boolean[] getVariableAssignment() throws GRBException {

        final double[] edgesAreUsed = new double[edgeIds.length];
        final int error = GurobiJni.getdblattrlist(model, "X", edgeIds.length, 0, edgeIds, edgesAreUsed);
        if (error != 0)
            throw new GRBException(GurobiJni.geterrormsg(GurobiJni.getenv(model)), error);

        final boolean[] assignments = new boolean[this.NumOfVariables];
        //final double tolerance = GurobiJniAccess.get(this.model, GRB.DoubleAttr.IntVio);
        for (int i = 0; i < assignments.length; ++i) {
            assert edgesAreUsed[i] > -0.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
            assert edgesAreUsed[i] < 1.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
            assignments[i] = edgesAreUsed[i] > 0.5d;
        }

        return assignments;
    }
}
