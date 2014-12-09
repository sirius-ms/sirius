package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TimeoutException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeScoring;
import gnu.trove.map.hash.TObjectIntHashMap;
import gurobi.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Spectar on 13.11.2014.
 */
public class NewGurobiSolver extends AbstractSolver {

    //TODO: Timelimit?
    final protected TreeBuilder feasibleSolver;

    protected final long model;
    protected final long envNative;

    protected int NumOfVariables;
    protected int NumOfVertices;
    protected int[] variables;
    protected double[] coefs;
    protected double[] constants;
    protected char[] signs;

    public NewGurobiSolver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit) throws GRBException {
        super(graph, input, lowerbound, feasibleSolver, timeLimit);

        this.envNative = getDefaultEnv(null);

        int[] error = new int[1];
        this.model = GurobiJni.newmodel(error, this.envNative, null, 0, null, null, null, null, null);
        GurobiJniAccess.set( this.envNative, GRB.DoubleParam.TimeLimit, 0);

        this.feasibleSolver = feasibleSolver;

        NumOfVertices = graph.numberOfVertices();
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
    /**
     * for each constraint i in array 'offsets': offsets[i] is the first index, where the constraint i is located
     * (inside 'var' and 'coefs')
     */
    protected void computeOffsets() {

        for (int k = 1; k < offsets.length; ++k)
            offsets[k] = offsets[k - 1] + graph.getFragmentAt(k - 1).getOutDegree();

        /*
         * for each edge: give it some unique id based on its source vertex id and its offset
         * therefor, the i-th edge of some vertex u will have the id: offsets[u] + i - 1, if i=1 is the first edge.
         * That way, 'edgeIds' is already sorted by source edge id's! An in O(E) time
          */
        for (int k = 0; k < losses.size(); ++k) {
            final int u = losses.get(k).getSource().getVertexId();
            edgeIds[offsets[u]++] = k;
        }

        // by using the loop-code above -> offsets[k] = 2*OutEdgesOf(k), so subtract that 2 away
        for (int k = 0; k < offsets.length; ++k)
            offsets[k] -= graph.getFragmentAt(k).getOutDegree();
            //TODO: optimize: offsets[k] /= 2;
    }


    @Override
    protected void setLowerbound() throws GRBException {
        if (lowerbound != 0) {
            GurobiJniAccess.set(this.model, GRB.DoubleParam.Cutoff, 0.0);
        }
    }


    @Override
    /**
     * variables are edges as {0.0, 1.0} and their weight as coefficients
     */
    protected void defineVariables() throws GRBException {
        // cannot really do sth. here
        NumOfVariables = this.losses.size();
        this.variables = new int[NumOfVariables];
    }


    @Override
    /**
     * this will add all necessary variables through the gurobi jni interface to be used later
     */
    protected void setVariablesWithOffset() throws GRBException {

        if (this.model == 0L) {
            throw new GRBException("Model not loaded", 20003);
        }

        // a variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}
        // In our case, many edges will not be present. It is sufficient enough to just store those existing ones
        // and remember their ids. That way, everything is stored in individual arrays, that are accessible through
        // 'offsets' and 'edgeIds'.

        double[] vals = new double[this.NumOfVariables];
        double[] ubs = new double[this.NumOfVariables];
        double[] lbs = null; // this will cause lbs to be 0.0 by default
        double[] obj = null; // arguments will have objective coefficients of 0.0 TODO: is that right?
        char[] vtypes = null; // arguments will be assumed to be continuous
        String[] names = null; // arguments will have default names.

        for(int i=0; i < losses.size(); i++) {
            ubs[i] = 1.0;
            vals[i] = -losses.get(i).getWeight();
        }

        int error = GurobiJni.addvars(model, NumOfVariables, 0, offsets, edgeIds, vals, obj , lbs, ubs, vtypes, names);
        if (error != 0) {
            throw new GRBException(GurobiJni.geterrormsg(this.envNative), error);
        }

    }


    @Override
    protected void setTreeConstraint() {

        // a variable is basically the existence of an edge {0.0, 1.0} multiplied with some coefficient {weight of edge}
        // In our case, many edges will not be present. It is sufficient enough to just store those existing ones
        // and remember their ids. That way, everything is stored in individual arrays, that are accessible through
        // 'offsets' and 'edgeIds'.

        // prepare arrays
        this.coefs = new double[NumOfVariables];
        this.signs = new char[NumOfVariables];
        this.constants = new double[NumOfVariables];

        for (int k=0; k<this.NumOfVertices; k++) {
            // NumOfVariables is NumOfEdges!
            final int LAST_INDEX = (k == this.NumOfVariables-1) ? NumOfVariables -1 : offsets[k+1] -1;

            for (int i=offsets[k]; i<LAST_INDEX; i++) {
                coefs[i] = losses.get(i).getWeight();
            }

            // we want to only use 1 edge from any vertex at max
            signs[k] = GRB.LESS_EQUAL;
            constants[k] = 1.0d;
        }

        //TODO: determine the second last double array! (last one is an array of constraint names)
        GurobiJni.addconstrs(model, offsets.length, coefs.length, offsets, edgeIds, coefs, signs, constants, null ,null);
    }


    @Override
    protected void setColorConstraint() {

    }


    @Override
    protected void setMinimalTreeSizeConstraint() {

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

        final double[] edgesAreUsed = GurobiJniAccess.getVariableAssignment(model, this.NumOfVariables);
        final boolean[] assignments = new boolean[this.NumOfVariables];
        final double tolerance = GurobiJniAccess.get(this.model, GRB.DoubleAttr.IntVio);
        for (int i = 0; i < assignments.length; ++i) {
            assert edgesAreUsed[i] > -0.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
            assert edgesAreUsed[i] < 1.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
            ;
            assignments[i] = (Math.round(edgesAreUsed[i]) == 1);
        }

        return assignments;
    }


    //-- THIS SECTION MAY BE PACKED INTO A SEPERATE PACKAGE --//
        //-- IT WILL ALLOW USING THE JNI INTERFACE DIRECTLY --//



}
