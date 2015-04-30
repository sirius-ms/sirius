package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import com.google.common.collect.BiMap;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TimeoutException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import gnu.trove.map.hash.TObjectIntHashMap;
import gurobi.*;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GurobiSolver implements TreeBuilder {

    private GRBEnv env;
    private TreeBuilder feasibleSolver;
    private int secondsPerInstance;
    private int secondsPerDecomposition;
    private long timeout;
    private int lastInput;
    private int numberOfCPUs;

    public GurobiSolver(GRBEnv env, TreeBuilder feasibleSolver) {
        this.env = env;
        this.feasibleSolver = feasibleSolver;
        this.secondsPerInstance = 18 * 60 * 60; // maximal 5 hour per instance
        this.secondsPerDecomposition = 18 * 60 * 60; // maximal 5 hours per decomposition
        this.lastInput = 0;
        this.timeout = System.currentTimeMillis();
    }

    public GurobiSolver() {
        this(getDefaultEnv(), null);
    }

    public GurobiSolver(TreeBuilder feasibleSolver) {
        this(getDefaultEnv(), feasibleSolver);
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

    protected static FTree newTree(FGraph graph, FTree tree, double rootScore) {
        return newTree(graph, tree, rootScore, rootScore);
    }

    protected static FTree newTree(FGraph graph, FTree tree, double rootScore, double scoring) {
        tree.addAnnotation(ProcessedInput.class, graph.getAnnotationOrThrow(ProcessedInput.class));
        tree.addAnnotation(Ionization.class, graph.getAnnotationOrThrow(Ionization.class));
        final TreeScoring treeScoring = new TreeScoring();
        tree.addAnnotation(TreeScoring.class, treeScoring);
        treeScoring.setOverallScore(scoring);
        treeScoring.setRootScore(rootScore);
        for (Map.Entry<Class<Object>, Object> entry : graph.getAnnotations().entrySet()) {
            tree.setAnnotation(entry.getKey(), entry.getValue());
        }
        if (graph.numberOfVertices() <= 2) {
            final Fragment graphVertex = graph.getFragmentAt(1);
            final Fragment treeVertex = tree.getFragmentAt(0);
            for (FragmentAnnotation<Object> x : graph.getFragmentAnnotations()) {
                tree.addFragmentAnnotation(x.getAnnotationType()).set(treeVertex, x.get(graphVertex));
            }
            for (LossAnnotation<Object> x : graph.getLossAnnotations()) {
                tree.addLossAnnotation(x.getAnnotationType()).set(treeVertex.getIncomingEdge(), x.get(graphVertex.getIncomingEdge()));
            }
        }
        return tree;
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

    public void resetTimeLimit() {
        timeout = System.currentTimeMillis() + secondsPerDecomposition * 1000l;
    }

    public void optimizeParameters(File file, ProcessedInput input, FGraph graph) {
        try {
            final Solver solver = new Solver(graph, input, 0d, env, feasibleSolver, Integer.MAX_VALUE);
            solver.defineVariables();
            solver.optimize();
            solver.computeOffsets();
            solver.setConstraints();
            solver.model.update();
            solver.model.tune();
            solver.model.getTuneResult(0);
            solver.model.write(file.getName());
        } catch (GRBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
        return buildTree(input, graph, lowerbound, prepareTreeBuilding(input, graph, lowerbound));
    }

    @Override
    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound) {
        if (lastInput != input.getExperimentInformation().hashCode()) {
            // reset time limit
            resetTimeLimit();
            lastInput = input.getExperimentInformation().hashCode();
        }
        try {
            if (graph.numberOfVertices() <= 2) {
                return newTree(graph, new FTree(graph.getRoot().getChildren(0).getFormula()), graph.getRoot().getOutgoingEdge(0).getWeight());
            }
            final long timeToCompute = Math.max(0l, Math.min((long) secondsPerDecomposition, timeout - System.currentTimeMillis()));
            final Solver solver = new Solver(graph, input, lowerbound, env, feasibleSolver,
                    (int) Math.min(Integer.MAX_VALUE, timeToCompute));
            solver.build();
            return solver;
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object prepared) {
        if (graph.numberOfVertices() <= 2)
            return newTree(graph, new FTree(graph.getRoot().getChildren(0).getFormula()), graph.getRoot().getOutgoingEdge(0).getWeight());
        if (!(prepared instanceof Solver))
            throw new IllegalArgumentException("Expected solver to be instance of Solver, but " + prepared.getClass() + " given.");
        final Solver solver = (Solver) prepared;
        return solver.solve();
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected static class Stackitem {
        private final Fragment treeNode;
        private final Fragment graphNode;

        protected Stackitem(Fragment treeNode, Fragment graphNode) {
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
        protected final FGraph graph;
        protected final ProcessedInput input;
        protected final TreeBuilder feasibleSolver;
        protected final int timeLimit;
        protected final double lowerbound;
        protected int identifier;
        protected boolean built;

        protected Solver(FGraph graph, ProcessedInput input, double lowerbound, GRBEnv env, TreeBuilder feasibleSolver, int timeLimit) throws GRBException {
            this.graph = graph;
            this.losses = new ArrayList<Loss>(graph.numberOfVertices() * 10);
            for (Fragment f : graph.getFragments()) {
                for (int l = 0; l < f.getInDegree(); ++l) losses.add(f.getIncomingEdge(l));
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
            built = false;
        }

        protected static boolean isComputationCorrect(FTree tree, FGraph graph) {
            double score = tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
            final Fragment pseudoRoot = graph.getRoot();
            final BiMap<Fragment, Fragment> fragmentMap = FTree.createFragmentMapping(tree, graph);
            for (Map.Entry<Fragment, Fragment> e : fragmentMap.entrySet()) {
                final Fragment t = e.getKey();
                final Fragment g = e.getValue();
                if (g.getParent() == pseudoRoot) {
                    score -= g.getIncomingEdge().getWeight();
                } else {
                    final Loss in = e.getKey().getIncomingEdge();
                    for (int k = 0; k < g.getInDegree(); ++k)
                        if (in.getSource().getFormula().equals(g.getIncomingEdge(k).getSource().getFormula())) {
                            score -= g.getIncomingEdge(k).getWeight();
                            break;
                        }
                }
            }
            return Math.abs(score) < 1e-9d;
        }

        public void build() {
            try {
                defineVariables();
                model.update();
                if (feasibleSolver != null) {
                    final FTree presolvedTree = feasibleSolver.buildTree(input, graph, lowerbound);
                    setStartValues(presolvedTree);
                }
                computeOffsets();
                setConstraints();
                //setLowerbound();
                assert model.get(GRB.IntAttr.IsMIP) != 0;
                built = true;
            } catch (GRBException e) {
                throw new RuntimeException(String.valueOf(e.getErrorCode()), e);
            }
        }

        protected FTree solve() {
            try {
                if (!built) build();
                optimize();
                if (model.get(GRB.IntAttr.Status) != GRB.OPTIMAL) {
                    if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
                        return null; // lowerbound reached
                    } else {
                        errorHandling();
                        return null;
                    }
                }
                final FTree tree = buildSolution();
                if (!isComputationCorrect(tree, graph)) {
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
            setMinimalTreeSizeConstraint();


            // HACK!
            //setMaxNumberOfPeaksConstraint();

        }

        private void setMinimalTreeSizeConstraint() throws GRBException {
            // tree have to consist of at least one vertex
            final GRBLinExpr expr = new GRBLinExpr();
            final Fragment pseudoRoot = graph.getRoot();
            final int from = offsets[pseudoRoot.getVertexId()];
            final int to = from + pseudoRoot.getOutDegree();
            for (int k = from; k < to; ++k) {
                expr.addTerm(1, variables[edgeIds[k]]);
            }
            model.addConstr(expr, GRB.GREATER_EQUAL, 1, null);
        }

        private void setMaxNumberOfPeaksConstraint() throws GRBException {
            final GRBLinExpr expr = new GRBLinExpr();
            for (int i = 0; i < variables.length; ++i) {
                expr.addTerm(1, variables[i]);
            }
            model.addConstr(expr, GRB.LESS_EQUAL, 30, null);
        }

        protected void errorHandling() throws GRBException {
            // infeasible solution!
            int status = model.get(GRB.IntAttr.Status);
            String cause = "";
            switch (status) {
                case GRB.TIME_LIMIT:
                    cause = "Timeout (exceed time limit of " + timeLimit + " seconds per decomposition";
                    throw new TimeoutException(cause);
                case GRB.INFEASIBLE:
                    cause = "Solution is infeasible.";
                    break;
                case GRB.CUTOFF:
                    return;
                default:
                    try {
                        if (model.get(GRB.DoubleAttr.ConstrVioSum) > 0)
                            cause = "Constraint are violated. Tree-correctness: "
                                    + isComputationCorrect(buildSolution(), graph);
                        else cause = "Unknown error. Status code is " + status;
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

        protected void setStartValues(FTree presolvedTree) throws GRBException {
            final TObjectIntHashMap<MolecularFormula> map = new TObjectIntHashMap<MolecularFormula>(presolvedTree.numberOfVertices() * 3, 0.5f, -1);
            int k = 0;
            for (Fragment f : presolvedTree.getFragments()) map.put(f.getFormula(), k++);
            final boolean[][] matrix = new boolean[k][k];
            for (Fragment f : presolvedTree.getFragmentsWithoutRoot())
                matrix[map.get(f.getFormula())][map.get(f.getIncomingEdge().getSource().getFormula())] = true;
            final double[] startValues = new double[losses.size()];
            for (int i = 0; i < losses.size(); ++i) {
                final Loss l = losses.get(i);
                final int u = map.get(l.getSource().getFormula());
                if (u < 0) continue;
                final int v = map.get(l.getTarget().getFormula());
                if (v < 0) continue;
                if (matrix[v][u]) startValues[i] = 1.0d;
            }
            model.set(GRB.DoubleAttr.Start, variables, startValues);
        }

        protected void computeOffsets() {
            for (int k = 1; k < offsets.length; ++k) {
                offsets[k] = offsets[k - 1] + graph.getFragmentAt(k - 1).getOutDegree();
            }
            for (int k = 0; k < losses.size(); ++k) {
                final int u = losses.get(k).getSource().getVertexId();
                edgeIds[offsets[u]++] = k;
            }
            for (int k = 0; k < offsets.length; ++k) {
                offsets[k] -= graph.getFragmentAt(k).getOutDegree();
            }
        }

        protected void setTreeConstraint() throws GRBException {
            int k = 0;
            for (Fragment fragment : graph.getFragmentsWithoutRoot()) {
                final GRBLinExpr expression = new GRBLinExpr();
                for (int l = 0; l < fragment.getInDegree(); ++l) {
                    expression.addTerm(1d, variables[k]);
                    ++k;
                }
                // 1. For all vertices u with u!=root the sum of all edges uv for a fixed v is <= 1
                // => TreeCondition: Each vertex (except root has a parent
                model.addConstr(expression, GRB.LESS_EQUAL, 1, null);//String.valueOf(identifier++));
                // 2. An edge is only set, if one of the incomming edges of its head is set
                // => Connectivity-Condition: There is a path between each two vertices
                int j = offsets[fragment.getVertexId()];
                for (int l = 0; l < fragment.getOutDegree(); ++l) {
                    model.addConstr(variables[edgeIds[j]], GRB.LESS_EQUAL, expression, null);//String.valueOf(identifier++));

                    assert losses.get(edgeIds[j]).getSource() == fragment;
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
            final GRBLinExpr[] colorExpressions = new GRBLinExpr[graph.maxColor() + 1];
            final boolean[] colorInUse = new boolean[colorExpressions.length];
            for (int i = 0; i < colorExpressions.length; ++i)
                colorExpressions[i] = new GRBLinExpr();
            int k = 0;
            for (Loss l : losses) {
                final int color = l.getTarget().getColor();
                colorInUse[color] = true;
                colorExpressions[color].addTerm(1, variables[k]);
                ++k;
            }
            /////////// DEBUG
            //colorConstraints = new GRBConstr[colorExpressions.length];
            //int x=0;
            ///////////
            for (int i = 0; i < colorExpressions.length; ++i) {
                //colorConstraints[x++] =/* debug */
                if (colorInUse[i])
                    model.addConstr(colorExpressions[i], GRB.LESS_EQUAL, 1, null);//String.valueOf(++identifier));
            }
        }

        protected void setLowerbound() throws GRBException {
            /*
            if (lowerbound != 0) {
                final GRBLinExpr lowerboundExpression = new GRBLinExpr();
                for (int i=0; i < losses.size(); ++i) {
                    assert !Double.isInfinite(-losses.get(i).getWeight()) && !Double.isNaN(-losses.get(i).getWeight());
                    lowerboundExpression.addTerm(-losses.get(i).getWeight(), vars[i]);
                }
                assert !Double.isInfinite(-lowerbound) && !Double.isNaN(-lowerbound);
                model.addConstr(lowerboundExpression, GRB.LESS_EQUAL, -lowerbound, null);//String.valueOf(++identifier));
            }
            */
            if (lowerbound != 0) {
                model.getEnv().set(GRB.DoubleParam.Cutoff, 0);
            }
        }

        protected void optimize() throws GRBException {
            model.optimize();
        }

        protected FTree buildSolution() throws GRBException {
            final double score = -model.get(GRB.DoubleAttr.ObjVal);

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

        protected boolean[] getVariableAssignment() throws GRBException {
            final double[] edgesAreUsed = model.get(GRB.DoubleAttr.X, variables);
            final boolean[] assignments = new boolean[variables.length];
            final double tolerance = model.get(GRB.DoubleAttr.IntVio);
            for (int i = 0; i < assignments.length; ++i) {
                assert edgesAreUsed[i] > -0.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
                assert edgesAreUsed[i] < 1.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
                assignments[i] = (Math.round(edgesAreUsed[i]) == 1);
            }
            return assignments;
        }
    }

}
