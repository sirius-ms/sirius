
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.sirius.ProcessedInput;
import gnu.trove.map.hash.TCustomHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Spectar on 13.11.2014.
 */
abstract public class AbstractSolver {

    // graph information
    protected final ProcessedInput input;
    protected final FGraph graph;
    protected final List<Loss> losses;
    protected final int[] edgeIds; // contains variable indices (after 'computeoffsets')
    protected final int[] edgeOffsets; // contains: the first index j of edges starting from a given vertex i

    protected TreeBuilder.FluentInterface options;

    ////////////////////////
    //--- CONSTRUCTORS ---//
    ////////////////////////

    protected AbstractSolver(FGraph graph, ProcessedInput input, TreeBuilder.FluentInterface options) {
        if (graph == null) throw new NullPointerException("Cannot solve graph: graph is NULL!");
        this.graph = graph;
        this.losses = new ArrayList<Loss>(graph.numberOfEdges());
        for (Fragment f : graph) {
            for (int k = 0; k < f.getInDegree(); ++k) {
                losses.add(f.getIncomingEdge(k));
            }
        }
        this.edgeIds = new int[graph.numberOfEdges()];
        this.edgeOffsets = new int[graph.numberOfVertices()];
        this.input = input;
        this.options = options;
    }

    public TreeBuilder.Result compute() {
        if (graph.numberOfEdges() == 1) {
            IntergraphMapping.Builder mapping = IntergraphMapping.build();
            FTree smallTree = buildSolution(graph.getRoot().getOutgoingEdge(0).getWeight(), new boolean[]{true}, mapping);
            return new TreeBuilder.Result(smallTree, true, TreeBuilder.AbortReason.COMPUTATION_CORRECT, mapping.done(graph,smallTree));
        }
        return solve();
    }

    protected abstract void setTimeLimitInSeconds(double timeLimitsInSeconds) throws Exception;

    protected abstract void setNumberOfCpus(int numberOfCPUS) throws Exception;


    /////////////////////
    ///--- METHODS ---///
    /////////////////////

    /**
     * - this class should be implemented through abstract sub methods
     * - model.update() like used within the gurobi solver may be used within one of those, if necessary
     */
    protected void prepareSolver() {
        try {
            initializeModel();
            if (options.getNumberOfCPUS() > 0)
                setNumberOfCpus(options.getNumberOfCPUS());
            if (options.getTimeLimitsInSeconds() > 0)
                setTimeLimitInSeconds(options.getTimeLimitsInSeconds());
            computeOffsets();
            assert (edgeOffsets != null && (edgeOffsets.length != 0 || losses.size() == 0)) : "Edge edgeOffsets were not calculated?!";

            defineVariables();
            if (options.getTemplate() != null) {
                setVariableStartValues(options.getTemplate());
            }

            setConstraints();
            setObjective();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(String.valueOf(e.getMessage()), e);
        }
    }

    protected abstract void initializeModel() throws Exception;


    /**
     * - edgeOffsets will be used to access edges more efficiently
     * for each constraint i in array 'edgeOffsets': edgeOffsets[i] is the first index, where the constraint i is located
     * (inside 'var' and 'coefs')
     * Additionally, a new loss array will be computed
     */
    protected final void computeOffsets() {

        for (int k = 1; k < edgeOffsets.length; ++k)
            edgeOffsets[k] = edgeOffsets[k - 1] + graph.getFragmentAt(k - 1).getOutDegree();

        /*
         * for each edge: give it some unique id based on its source vertex id and its offset
         * therefor, the i-th edge of some vertex u will have the id: edgeOffsets[u] + i, if i=0 is the first edge.
         * That way, 'edgeIds' is already sorted by source edge id's! An in O(E) time
         */
        for (int k = 0; k < losses.size(); ++k) {
            final int u = losses.get(k).getSource().getVertexId();
            edgeIds[edgeOffsets[u]++] = k;
        }

        // by using the loop-code above -> edgeOffsets[k] = 2*OutEdgesOf(k), so subtract that 2 away
        for (int k = 0; k < edgeOffsets.length; ++k)
            edgeOffsets[k] -= graph.getFragmentAt(k).getOutDegree();
        //TODO: optimize: edgeOffsets[k] /= 2;
    }


    //-- Methods to initiate the solver
    //-- Exception types may be override within subclasses, if needed

    protected void setConstraints() throws Exception {
        setTreeConstraint();
        setColorConstraint();
        setMinimalTreeSizeConstraint();
        if (!Double.isInfinite(options.getMinimalScore()))
            setMinimalScoreConstraints(options.getMinimalScore());
    }

    /**
     * - The sum of all edges kept in the solution (if existing) should be at least as high as the given lower bound
     * - This information might be used by a solver to stop the calculation, when it is obviously not possible to
     * reach that condition.
     *
     * @throws Exception
     */
    protected abstract void setMinimalScoreConstraints(double minimalScore) throws Exception;


    /**
     * Solve the optimal colorful subtree problem, using the chosen solver
     * Need constraints, variables, etc. to be set up
     *
     * @return
     */
    protected TreeBuilder.Result solve() {
        try {
            prepareSolver();
            // get optimal solution (score) if existing
            TreeBuilder.AbortReason c = solveMIP();

            if (c == TreeBuilder.AbortReason.COMPUTATION_CORRECT) {
                // reconstruct tree after having determined the (possible) optimal solution
                final double score = getSolverScore();
                final IntergraphMapping.Builder builder = IntergraphMapping.build();
                final FTree tree = buildSolution(builder);
                IntergraphMapping done = builder.done(graph, tree);
                if (tree != null && !isComputationCorrect(tree, this.graph, score,done))
                    throw new RuntimeException("Can't find a feasible solution: Solution is buggy");
                return new TreeBuilder.Result(tree, true, c, done);
            } else if (c == TreeBuilder.AbortReason.TIMEOUT) {
                throw new TimeoutException("ILP Solver canceled by Timeout!");
            } else return new TreeBuilder.Result(null, false, c,null);
        } catch (Exception e) {
            if (e instanceof TimeoutException) throw (TimeoutException) e;
            throw new RuntimeException(String.valueOf(e.getMessage()), e);
        } finally {
            // free any memory, if necessary
            try {
                pastBuildSolution();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    // functions used within 'prepareSolver'

    /**
     * Variables in our problem are the edges of the given graph.
     * In the solution, 0.0 means: edge is not used, while 1.0 means: edge is used
     *
     * @throws Exception
     */
    abstract protected void defineVariables() throws Exception;

    protected void setVariableStartValues(FTree presolvedTree) throws Exception {
        // map edges in presolved tree to edge ids
//        final HashMap<MolecularFormula, Fragment> fragmentMap = new HashMap<>(presolvedTree.numberOfVertices());
        final TCustomHashMap<Fragment, Fragment> fragmentMap = Fragment.newFragmentWithIonMap();
        for (Fragment f : presolvedTree) fragmentMap.put(f, f);

        int[] selectedEdges = new int[1 + presolvedTree.numberOfEdges()];
        int k = 0, offset = 0;

        // find pseudo root
        final MolecularFormula root = presolvedTree.getRoot().getFormula();
        for (int l = 0; l < graph.getRoot().getOutDegree(); ++l) {
            if (losses.get(edgeIds[l]).getTarget().getFormula().equals(root)) {
                selectedEdges[k++] = edgeIds[l];
                break;
            }
        }

        forEachFragment:
        for (int i = 1; i < this.graph.numberOfVertices(); ++i) {
            final Fragment fragment = this.graph.getFragmentAt(i);
            if (fragment.getFormula().isEmpty())
                continue;
//            final Fragment treeFragment = fragmentMap.get(fragment.getFormula());
            final Fragment treeFragment = fragmentMap.get(fragment);
            if (treeFragment != null && !treeFragment.isRoot()) {
                final MolecularFormula lf = treeFragment.getIncomingEdge().getFormula();
                // find corresponding loss
                forEachLoss:
                for (int l = 0; l < fragment.getInDegree(); ++l) {
                    if (fragment.getIncomingEdge(l).getFormula().equals(lf)) {
                        // we find the correct edge
                        selectedEdges[k++] = offset + l;
                        break forEachLoss;
                    }
                }
            }
            offset += fragment.getInDegree();
        }

        if (k < selectedEdges.length)
            selectedEdges = Arrays.copyOf(selectedEdges, k);
        setVariableStartValues(selectedEdges);
    }

    protected abstract void setVariableStartValues(int[] usedEdgeIds) throws Exception;

    /**
     * - relaxed version: for each vertex, there are only one or more outgoing edges,
     * if there is at least one incomming edge
     * {@literal ->} the sum of all incomming edges - sum of outgoing edges {@literal >=} 0
     * - applying 'ColorConstraint' will tighten this condition to:
     * for each vertex, there can only be one incommning edge at most and only if one incomming edge is present,
     * one single outgoing edge can be present.
     *
     * @throws Exception
     */
    abstract protected void setTreeConstraint() throws Exception;

    /**
     * - for each color, take only one incoming edge
     * - the sum of all edges going into color relative is equal or less than 1
     *
     * @throws Exception
     */
    abstract protected void setColorConstraint() throws Exception;

    /**
     * - there should be at least one edge leading away from the root
     *
     * @throws Exception
     */
    abstract protected void setMinimalTreeSizeConstraint() throws Exception;

    // functions used within 'solve'

    /**
     * maximize a function z, where z is the sum of edges (as integer) multiplied by their weights
     * thus, this is a MIP problem, where the existence of edges in the solution is to be determined
     *
     * @throws Exception
     */
    abstract protected void setObjective() throws Exception;

    /**
     * - in here, the implemented solver should solve the problem, so that the result can be prepareSolver afterwards
     * - a specific solver might need to set up more before starting the solving process
     * - this is called after all constraints are applied
     *
     * @return
     * @throws Exception
     */
    abstract protected TreeBuilder.AbortReason solveMIP() throws Exception;

    /**
     * - a specific solver might need to do more (or release memory) after the solving process
     * - this is called after the solver() has been executed
     *
     * @throws Exception
     */
    abstract protected void pastBuildSolution() throws Exception;

    /**
     * - having found a solution using 'solveMIP' this function shall return a boolean list representing
     * those edges being kept in the solution.
     * - result[i] == TRUE means the i-th edge is included in the solution, FALSE otherwise
     *
     * @return
     * @throws Exception
     */
    abstract protected boolean[] getVariableAssignment() throws Exception;

    /**
     * - having found a solution using 'solveMIP' this function shall return the score of that solution
     * (basically, the accumulated weight at the root of the resulting tree or the value of the maximized objective
     * function, respectively)
     *
     * @return
     * @throws Exception
     */
    abstract protected double getSolverScore() throws Exception;


    protected FTree buildSolution(double score, boolean[] edesAreUsed, IntergraphMapping.Builder builder) {
        Fragment graphRoot = null;
        double rootScore = 0d;
        // get root
        {
            int offset = edgeOffsets[graph.getRoot().getVertexId()];
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

        final FTree tree = new FTree(graphRoot.getFormula(), graphRoot.getIonization());
        builder.mapLeftToRight(graphRoot,tree.getRoot());
        tree.setRootScore(rootScore);
        final ArrayDeque<Stackitem> stack = new ArrayDeque<Stackitem>();
        stack.push(new Stackitem(tree.getRoot(), graphRoot));
        while (!stack.isEmpty()) {
            final Stackitem item = stack.pop();
            final int u = item.graphNode.getVertexId();
            int offset = edgeOffsets[u];
            for (int j = 0; j < item.graphNode.getOutDegree(); ++j) {
                if (edesAreUsed[edgeIds[offset]]) {
                    final Loss l = losses.get(edgeIds[offset]);
                    final Fragment child = tree.addFragment(item.treeNode, l.getTarget());
                    builder.mapLeftToRight(l.getTarget(),child);
                    child.setColor(l.getTarget().getColor());
                    child.setPeakId(l.getTarget().getPeakId());
                    child.getIncomingEdge().setWeight(l.getWeight());
                    tree.setTreeWeight(tree.getTreeWeight() + l.getWeight());
                    stack.push(new Stackitem(child, l.getTarget()));
                }
                ++offset;
            }
        }
        tree.setTreeWeight(tree.getTreeWeight() + tree.getRootScore());
        return tree;
    }

    protected FTree buildSolution(IntergraphMapping.Builder builder) throws Exception {
        final double score = getSolverScore();

        final boolean[] edesAreUsed = getVariableAssignment();
        return buildSolution(score, edesAreUsed, builder);
    }

    ///////////////////////////
    ///--- CLASS-METHODS ---///
    ///////////////////////////

    /**
     * Check, whether or not the given tree 'tree' is the optimal solution for the optimal colorful
     * subtree problem of the given graph 'graph'
     *
     * @param tree
     * @param graph
     * @return
     */
    protected static boolean isComputationCorrect(FTree tree, FGraph graph, double score,IntergraphMapping mapping) {
        final double optSolScore = score;
        final Fragment pseudoRoot = graph.getRoot();
        for (Fragment t : tree) {
            final Fragment g = mapping.mapRightToLeft(t);
            if (g.getParent() == pseudoRoot) {
                score -= g.getIncomingEdge().getWeight();
            } else {
                //if (t.getFormula().isEmpty()) continue;
                final Loss in = t.getIncomingEdge();
                for (int k = 0; k < g.getInDegree(); ++k)
//                    if (in.getSource().getFormula().equals(g.getIncomingEdge(k).getSource().getFormula())) {
                    if (in.getSource().getFormula().equals(g.getIncomingEdge(k).getSource().getFormula()) && in.getSource().getIonization().equals(g.getIncomingEdge(k).getSource().getIonization())) {
                        score -= g.getIncomingEdge(k).getWeight();
                    }
            }
        }
        // just trust pseudo edges
        /*
        for (Fragment pseudo : tree.getFragments()) {
            if (pseudo.getFormula().isEmpty()) {
                score -= pseudo.getIncomingEdge().getWeight();
            }
        }
        */
        if (score > 1e-9d) {
            logger.warn("There is a large gap between the optimal solution and the score of the computed fragmentation tree: Gap is " + score + " for a score of " + optSolScore);
        }
        return Math.abs(score) < 1e-4d;
    }

    private static Logger logger = LoggerFactory.getLogger(AbstractSolver.class);

    protected static class Stackitem {
        protected final Fragment treeNode;
        protected final Fragment graphNode;

        protected Stackitem(Fragment treeNode, Fragment graphNode) {
            this.treeNode = treeNode;
            this.graphNode = graphNode;
        }
    }

}
