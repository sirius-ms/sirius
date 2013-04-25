package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.functional.iterator.Iterators;
import gurobi.*;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class GurobiSolver implements TreeBuilder {

    private GRBEnv env;
    private TreeBuilder feasibleSolver;
    private int secondsPerInstance;
    private int secondsPerDecomposition;
    private int freeSeconds;
    private int lastInput;
    private int numberOfCPUs;

    public GurobiSolver(GRBEnv env, TreeBuilder feasibleSolver) {
        this.env = env;
        this.feasibleSolver = feasibleSolver;
        this.secondsPerInstance = 60*30; // maximal 1/2 hour per instance
        this.secondsPerDecomposition = 30*60; // maximal 5 minutes per decomposition
        this.lastInput = 0;
        this.freeSeconds = 0;
    }

    public GurobiSolver() {
        this(getDefaultEnv(), null);
    }

    public GurobiSolver(TreeBuilder feasibleSolver) {
        this(getDefaultEnv(), feasibleSolver);
    }

    public int getSecondsPerDecomposition() {
        return secondsPerDecomposition;
    }

    public void setSecondsPerDecomposition(int secondsPerDecomposition) {
        this.secondsPerDecomposition = secondsPerDecomposition;
    }

    public int getNumberOfCPUs() {
        return numberOfCPUs;
    }

    public void setNumberOfCPUs(int numberOfCPUs) {
        if (numberOfCPUs != this.numberOfCPUs) {
            try {
                env.set(GRB.IntParam.Threads, numberOfCPUs);
            } catch (GRBException e) {
                throw new RuntimeException(e);
            }
        }
        this.numberOfCPUs = numberOfCPUs;

    }

    public GRBEnv getEnv() {
        return env;
    }

    public void setEnv(GRBEnv env) {
        this.env = env;
    }

    public TreeBuilder getFeasibleSolver() {
        return feasibleSolver;
    }

    public void setFeasibleSolver(TreeBuilder feasibleSolver) {
        this.feasibleSolver = feasibleSolver;
    }

    public int getSecondsPerInstance() {
        return secondsPerInstance;
    }

    public void setSecondsPerInstance(int secondsPerInstance) {
        this.secondsPerInstance = secondsPerInstance;
    }

    public static GRBEnv getDefaultEnv() {
        try {
            final GRBEnv env = new GRBEnv();
            env.set(GRB.IntParam.OutputFlag, 0);
            return env;
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FragmentationTree buildTree(ProcessedInput input, FragmentationGraph graph, double lowerbound) {
        if (lastInput != input.getExperimentInformation().hashCode()) {
            // reset time limit
            freeSeconds = secondsPerInstance;
            lastInput = input.getExperimentInformation().hashCode();
        }

        try {
            if (graph.numberOfVertices() == 1) return new FragmentationTree(graph.getRootScore(), graph);
            final long time = System.nanoTime();
            final int timeToCompute = Math.min(secondsPerDecomposition, freeSeconds);
            if (timeToCompute <= 0) throw new RuntimeException("Timeout: Exceeds time limit of " + secondsPerInstance + " seconds per instance");
            final FragmentationTree tree = new Solver(graph, input, lowerbound, env, feasibleSolver,
            		timeToCompute).solve();
            final long time2 = System.nanoTime();
            freeSeconds -= (time2-time)/1000000000;
            return tree;
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }

    protected static class Stackitem {
        private final TreeFragment treeNode;
        private final GraphFragment graphNode;

        protected Stackitem(TreeFragment treeNode, GraphFragment graphNode) {
            this.treeNode = treeNode;
            this.graphNode = graphNode;
        }
    }

    protected static class Solver {

    	protected final GRBModel model;
    	protected final GRBVar[] variables;
    	protected final int[] offsets;
    	protected final int[] edgeIds; // edgeIds[offset[u]] contains the id of the first outgoing edge of u
    	protected final ArrayList<Loss> losses;
    	protected final FragmentationGraph graph;
    	protected int identifier;
    	protected final ProcessedInput input;
    	protected final TreeBuilder feasibleSolver;
    	protected final int timeLimit;
        protected final double lowerbound;

        protected Solver(FragmentationGraph graph, ProcessedInput input, double lowerbound, GRBEnv env, TreeBuilder feasibleSolver, int timeLimit) throws GRBException {
            this.graph = graph;
            this.losses = new ArrayList<Loss>();
            for (Fragment f : graph.getFragments()) {
                for (Loss l : f.getIncomingEdges()) losses.add(l);
            }
            this.lowerbound = lowerbound;
            this.model = new GRBModel(env);
            this.variables = new GRBVar[losses.size()];
            this.offsets = new int[graph.numberOfVertices()];
            this.edgeIds = new int[losses.size()];
            this.identifier = 0;
            this.input = input;
            this.feasibleSolver = feasibleSolver;
            this.timeLimit = timeLimit;
            model.getEnv().set(GRB.DoubleParam.TimeLimit, timeLimit);
        }

        protected FragmentationTree solve() {
            try {
                defineVariables();
                optimize();
                if (feasibleSolver != null) {
                    final FragmentationTree presolvedTree = feasibleSolver.buildTree(input, graph, lowerbound);
                    setStartValues(presolvedTree);
                }
                computeOffsets();
                setConstraints();
                model.update();
                assert model.get(GRB.IntAttr.IsMIP) != 0;
                optimize();
                if (model.get(GRB.IntAttr.Status) != GRB.OPTIMAL) {
                    if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                        return null; // lowerbound reached
                    } else {
                        errorHandling();
                    }
                }
                final FragmentationTree tree = buildSolution();
                if (!tree.isComputationCorrect(graph.getRootScore())) {
                    throw new RuntimeException("Can't find a feasible solution: Solution is buggy");
                }
                model.dispose(); // free memory
                return tree;
            } catch (GRBException e) {
                throw new RuntimeException(String.valueOf(e.getErrorCode()), e);
            }
        }

        protected void setConstraints() throws GRBException {
        	setTreeConstraint();
            setColorConstraint();
            setLowerbound();
		}

		protected void errorHandling() throws GRBException{
            // infeasible solution!
            int status = model.get(GRB.IntAttr.Status);
            String cause = "";
            switch (status)  {
                case GRB.TIME_LIMIT: cause = "Timeout (exceed time limit of " + timeLimit + " seconds per decomposition"; break;
                case GRB.INFEASIBLE: cause = "Solution is infeasible."; break;
                default: try {
                    if (model.get(GRB.DoubleAttr.ConstrVioSum) > 0) cause = "Constraint are violated. Tree-correctness: "
                            + buildSolution().isComputationCorrect(graph.getRootScore());
                } catch (GRBException e) {
                    throw new RuntimeException("Unknown error. Status code is " + status, e);
                }
            }

            throw new RuntimeException("Can't find a feasible solution: " + cause);
        }

        protected void defineVariables() throws GRBException {
            for (int i = 0; i < losses.size(); ++i) {
                variables[i] = model.addVar(0.0, 1.0, -losses.get(i).getWeight(), GRB.INTEGER, null);//String.valueOf(identifier++));
            }
            model.update();
        }

        protected void setStartValues(FragmentationTree presolvedTree) throws GRBException {
            final double[] vector = new double[variables.length];
            final boolean[][] adjacenceMatrix = new boolean[graph.numberOfVertices()][graph.numberOfVertices()];
            for (Loss l : Iterators.asIterable(presolvedTree.lossIterator())) {
                adjacenceMatrix[l.getHead().getIndex()][l.getTail().getIndex()] = true;
            }
            for (int i=0; i < losses.size(); ++i) {
                final Loss uv = losses.get(i);
                if (adjacenceMatrix[uv.getHead().getIndex()][uv.getTail().getIndex()]) {
                    vector[i] = 1;
                }
            }
            model.set(GRB.DoubleAttr.Start, variables, vector);
        }

        protected void computeOffsets() {
            for (int k = 1; k < offsets.length; ++k) {
                offsets[k] = offsets[k - 1] + graph.getFragment(k - 1).numberOfChildren();
            }
            for (int k = 0; k < losses.size(); ++k) {
                final int u = losses.get(k).getHead().getIndex();
                edgeIds[offsets[u]++] = k;
            }
            for (int k=0; k < offsets.length; ++k) {
                offsets[k] -= graph.getFragment(k).numberOfChildren();
            }
        }

        protected void setTreeConstraint() throws GRBException {
            int k = 0;
            for (GraphFragment fragment : graph.getFragmentsWithoutRoot()) {
                final GRBLinExpr expression = new GRBLinExpr();
                for (Loss l : fragment.getIncomingEdges()) {
                    expression.addTerm(1d, variables[k]);
                    ++k;
                }
                // 1. For all vertices u with u!=root the sum of all edges uv for a fixed v is <= 1
                // => TreeCondition: Each vertex (except root has a parent
                model.addConstr(expression, GRB.LESS_EQUAL, 1, null);//String.valueOf(identifier++));
                // 2. An edge is only set, if one of the incomming edges of its head is set
                // => Connectivity-Condition: There is a path between each two vertices
                int j = offsets[fragment.getIndex()];
                for (Loss l : fragment.getOutgoingEdges()) {
                    model.addConstr(variables[edgeIds[j]], GRB.LESS_EQUAL, expression, null);//String.valueOf(identifier++));

                    assert losses.get(edgeIds[j]).getHead() == fragment;
                    ++j;
                }
                
                /*	DEBUG
                {
                    int x = offsets[fragment.getIndex()];
                    final HashSet<Loss> losses1 = new HashSet<Loss>();
                    final HashSet<Loss> losses2 = new HashSet<Loss>();
                    for (Loss l : fragment.getOutgoingEdges()) {
                        losses1.add(l);
                        losses2.add(losses.get(edgeIds[x++]));
                    }
                    assert losses1.equals(losses2);
                }
				*/
            }
        }

        //private GRBConstr[] colorConstraints;

        protected void setColorConstraint() throws GRBException {
            final GRBLinExpr[] colorExpressions = new GRBLinExpr[graph.numberOfColors()];
            final boolean[] colorInUse = new boolean[colorExpressions.length];
            for (int i=0; i < colorExpressions.length; ++i)
                colorExpressions[i] = new GRBLinExpr();
            int k=0;
            for (Loss l : losses) {
                final int color = l.getTail().getColor();
                colorInUse[color] = true;
                colorExpressions[color].addTerm(1, variables[k]);
                ++k;
            }
            /////////// DEBUG
            //colorConstraints = new GRBConstr[colorExpressions.length];
            //int x=0;
            ///////////
            for (int i=0; i < colorExpressions.length; ++i) {
                //colorConstraints[x++] =/* debug */
                if (colorInUse[i])
                    model.addConstr(colorExpressions[i], GRB.LESS_EQUAL, 1, null);//String.valueOf(++identifier));
            }
        }

        protected void setLowerbound() throws GRBException {
            if (lowerbound != 0) {
                final GRBLinExpr lowerboundExpression = new GRBLinExpr();
                for (int i=0; i < losses.size(); ++i) {
                    assert !Double.isInfinite(-losses.get(i).getWeight()) && !Double.isNaN(-losses.get(i).getWeight());
                    lowerboundExpression.addTerm(-losses.get(i).getWeight(), variables[i]);
                }
                assert !Double.isInfinite(-lowerbound) && !Double.isNaN(-lowerbound);
                model.addConstr(lowerboundExpression, GRB.LESS_EQUAL, -lowerbound, null);//String.valueOf(++identifier));
            }
        }

        protected void optimize() throws GRBException {
            model.optimize();
        }

        protected FragmentationTree buildSolution() throws GRBException {
            final double score = -model.get(GRB.DoubleAttr.ObjVal);

            final boolean[] edesAreUsed = getVariableAssignment();
            final FragmentationTree tree = new FragmentationTree(score + graph.getRootScore(), graph);
            final ArrayDeque<Stackitem> stack = new ArrayDeque<Stackitem>();
            stack.push(new Stackitem(tree.getRoot(), graph.getRoot()));
            while (!stack.isEmpty()) {
                final Stackitem item = stack.pop();
                final int u = item.graphNode.getIndex();
                int offset = offsets[u];
                for (int j=0; j < item.graphNode.numberOfChildren(); ++j) {
                    if (edesAreUsed[edgeIds[offset]]) {
                        final Loss l = losses.get(edgeIds[offset]);
                        stack.push(new Stackitem(tree.addVertex(item.treeNode, l), (GraphFragment) l.getTail()));
                    }
                    ++offset;
                }
            }
            return tree;
        }

        protected boolean[] getVariableAssignment() throws GRBException {
            final double[] edgesAreUsed = model.get(GRB.DoubleAttr.X, variables);
            final boolean[] assignments = new boolean[variables.length];
            final double tolerance = model.get(GRB.DoubleAttr.IntVio);
            for (int i=0; i < assignments.length; ++i) {
                assert edgesAreUsed[i] >= 0 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
                assert edgesAreUsed[i] <= 1 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];;
                assignments[i] = (Math.round(edgesAreUsed[i]) == 1);
            }
            return assignments;
        }

    }

}
