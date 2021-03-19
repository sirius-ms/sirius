package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.sirius.ProcessedInput;
import gurobi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrbSolver extends AbstractSolver{
    public final static IlpFactory<GrbSolver> Factory = new IlpFactory<>() {
        protected GRBEnv env = getDefaultEnv();

        @Override
        public GrbSolver create(ProcessedInput input, FGraph graph, TreeBuilder.FluentInterface options) {
            return new GrbSolver(env, graph, input, options);
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }

        @Override
        public String name() {
            return "Gurobi";
        }

        @Override
        public void checkSolver() throws ILPSolverException {
            try {
                new GRBModel(env).getEnv();
            } catch (Throwable e) {
                throw new ILPSolverException(e);
            }
        }
    };

    protected GRBModel model;
    protected GRBVar[] variables;
    protected GRBEnv env;

    public GrbSolver(GRBEnv env, FGraph graph, ProcessedInput input, TreeBuilder.FluentInterface options) {
        super(graph, input, options);
        this.env = env;
    }

    @Override
    protected void setTimeLimitInSeconds(double timeLimitsInSeconds) throws Exception {
        model.getEnv().set(GRB.DoubleParam.TimeLimit, timeLimitsInSeconds);
    }

    @Override
    protected void setNumberOfCpus(int numberOfCPUS) throws Exception {
        env.set(GRB.IntParam.Threads, numberOfCPUS);
    }

    @Override
    protected void initializeModel() throws Exception {
        this.model = new GRBModel(env);
        this.variables = new GRBVar[losses.size()];
    }

    @Override
    protected void setMinimalScoreConstraints(double minimalScore) throws Exception {
        model.getEnv().set(GRB.DoubleParam.Cutoff, -minimalScore);
    }

    @Override
    protected void defineVariables() throws Exception {
        for (int i = 0; i < losses.size(); ++i) {
            variables[i] = model.addVar(0.0, 1.0, -losses.get(i).getWeight(), GRB.INTEGER, null);
        }
        model.update();
    }

    @Override
    protected void setVariableStartValues(int[] usedEdgeIds) throws Exception {
        final double[] values = new double[losses.size()];
        for (int index : usedEdgeIds) values[index] = 1d;
        model.set(GRB.DoubleAttr.Start, variables, values);
    }

    @Override
    protected void setTreeConstraint() throws Exception {
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
            int j = edgeOffsets[fragment.getVertexId()];
            for (int l = 0; l < fragment.getOutDegree(); ++l) {
                model.addConstr(variables[edgeIds[j]], GRB.LESS_EQUAL, expression, null);
                assert losses.get(edgeIds[j]).getSource() == fragment;
                ++j;
            }
        }
    }

    @Override
    protected void setColorConstraint() throws Exception {
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
        for (int i = 0; i < colorExpressions.length; ++i) {
            if (colorInUse[i])
                model.addConstr(colorExpressions[i], GRB.LESS_EQUAL, 1, null);
        }
    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {
        // tree have to consist of at least one vertex
        final GRBLinExpr expr = new GRBLinExpr();
        final Fragment pseudoRoot = graph.getRoot();
        final int from = edgeOffsets[pseudoRoot.getVertexId()];
        final int to = from + pseudoRoot.getOutDegree();
        for (int k = from; k < to; ++k) {
            expr.addTerm(1, variables[edgeIds[k]]);
        }
        model.addConstr(expr, GRB.GREATER_EQUAL, 1, null);
    }

    @Override
    protected void setObjective() throws Exception {
        // already done
    }

    protected static final Logger logger = LoggerFactory.getLogger(GrbSolver.class);

    @Override
    protected TreeBuilder.AbortReason solveMIP() throws Exception {
        model.optimize();
        int status = model.get(GRB.IntAttr.Status);
        switch (status) {
            case GRB.OPTIMAL:
                return TreeBuilder.AbortReason.COMPUTATION_CORRECT;
            case GRB.TIME_LIMIT:
                return TreeBuilder.AbortReason.TIMEOUT;


            case GRB.INFEASIBLE:
                logger.info("Solution is infeasible.");
                return TreeBuilder.AbortReason.INFEASIBLE;

            case GRB.CUTOFF:
                return TreeBuilder.AbortReason.NO_SOLUTION;
            default:
                try {
                    if (model.get(GRB.DoubleAttr.ConstrVioSum) > 0) {
                        IntergraphMapping.Builder build = IntergraphMapping.build();
                        FTree tree = buildSolution(build);
                        logger.error("Constraint are violated. Tree-correctness: "
                                + isComputationCorrect(tree, graph, getSolverScore(),build.done(graph,tree)));
                    }
                    else logger.error("Unknown error. Status code is " + status);
                    return TreeBuilder.AbortReason.INFEASIBLE;
                } catch (GRBException e) {
                    throw new RuntimeException("Unknown error. Status code is " + status, e);
                }
        }
    }

    @Override
    protected void pastBuildSolution() throws Exception {
        model.dispose();
        //env.dispose();
    }

    @Override
    protected boolean[] getVariableAssignment() throws Exception {
        final double[] edgesAreUsed = model.get(GRB.DoubleAttr.X, variables);
        final boolean[] assignments = new boolean[variables.length];
        for (int i = 0; i < assignments.length; ++i) {
            assert edgesAreUsed[i] > -0.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
            assert edgesAreUsed[i] < 1.5 : "lowerbound violation for var " + i + " with value " + edgesAreUsed[i];
            assignments[i] = (Math.round(edgesAreUsed[i]) == 1);
        }
        return assignments;
    }

    @Override
    protected double getSolverScore() throws Exception {
        return -model.get(GRB.DoubleAttr.ObjVal);
    }

    private static GRBEnv getDefaultEnv() {
        try {
            final GRBEnv env = new GRBEnv();
            env.set(GRB.IntParam.OutputFlag, 0);
            return env;
        } catch (GRBException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
