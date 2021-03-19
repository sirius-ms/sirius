



package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.sirius.ProcessedInput;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloObjectiveSense;
import ilog.cplex.IloCplex;

import java.util.Arrays;

/**
 * Created by kaidu on 05.01.16.
 */
public class CPLEXSolver extends AbstractSolver {

    protected IloCplex model;
    protected IloIntVar[] variables;
    protected IloLPMatrix constraints;

    public final static IlpFactory<CPLEXSolver> Factory = new IlpFactory<>() {
        @Override
        public CPLEXSolver create(ProcessedInput input, FGraph graph, TreeBuilder.FluentInterface options) {
            return new CPLEXSolver(graph, input, options);
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }

        @Override
        public String name() {
            return "CPLEX";
        }

        @Override
        public void checkSolver() throws ILPSolverException {
            try {
                new IloCplex();
            } catch (Throwable e) {
                throw new ILPSolverException(e);
            }
        }
    };

    protected CPLEXSolver(FGraph graph, ProcessedInput input, TreeBuilder.FluentInterface options) {
        super(graph, input, options);
    }

    @Override
    protected void setTimeLimitInSeconds(double timeLimitsInSeconds) throws Exception{
        if (timeLimitsInSeconds>0) {
            model.setParam(IloCplex.Param.TimeLimit, timeLimitsInSeconds);
        }
    }

    @Override
    protected void setNumberOfCpus(int numberOfCPUS) throws Exception {
        if (numberOfCPUS>0) {
            model.setParam(IloCplex.Param.Threads, numberOfCPUS);
        } else throw new IllegalArgumentException("Invalid number of CPUs: " + numberOfCPUS);
    }

    @Override
    protected void initializeModel() throws Exception {
        model = new IloCplex();
        model.setOut(null);
        model.setParam(IloCplex.IntParam.Threads, 1);
        constraints = model.addLPMatrix();
    }

    @Override
    protected void setMinimalScoreConstraints(double minimalScore) throws Exception {
        model.setParam(IloCplex.Param.MIP.Tolerances.LowerCutoff, minimalScore);
    }

    @Override
    protected void defineVariables() throws Exception {
        variables = model.boolVarArray(losses.size());
        constraints.addCols(variables);

    }

    @Override
    protected void setVariableStartValues(int[] selectedEdges) throws Exception {
        final double[] values = new double[variables.length];
        for (int index : selectedEdges) {
            values[index] = 1d;
        }
        model.addMIPStart(variables, values, IloCplex.MIPStartEffort.CheckFeas);
    }


    protected void setTreeConstraint() throws IloException {
        int k = 0;
        for (Fragment fragment : graph.getFragmentsWithoutRoot()) {
            final int[] indizesLeft = new int[fragment.getInDegree()];
            final double[] onesLeft = new double[fragment.getInDegree()];
            Arrays.fill(onesLeft, 1d);
            for (int l = 0; l < fragment.getInDegree(); ++l) {
                indizesLeft[l] = k++;
            }
            // 1. For all vertices u with u!=root the sum of all edges uv for a fixed v is <= 1
            // => TreeCondition: Each vertex (except root has a parent
            constraints.addRow(0, 1, indizesLeft, onesLeft);
            //model.addConstr(expression, GRB.LESS_EQUAL, 1, null);//String.valueOf(identifier++));
            // 2. An edge is only set, if one of the incomming edges of its head is set
            // => Connectivity-Condition: There is a path between each two vertices
            int j = edgeOffsets[fragment.getVertexId()];

            final int[] indizes2 = Arrays.copyOf(indizesLeft, indizesLeft.length+1);
            final double[] row2 = Arrays.copyOf(onesLeft, onesLeft.length+1);
            final int n = indizesLeft.length;
            row2[n] = -1d;
            for (int l = 0; l < fragment.getOutDegree(); ++l) {
                indizes2[n] = edgeIds[j];
                constraints.addRow(0, 1, indizes2, row2);
                assert losses.get(edgeIds[j]).getSource() == fragment;
                ++j;
            }
        }
    }

    @Override
    protected void setColorConstraint() throws IloException {

        final int[] colorSizes = new int[graph.maxColor()+1];
        for (Loss l : losses) {
            ++colorSizes[l.getTarget().getColor()];
        }
        final int[][] indizesPerColor = new int[colorSizes.length][];
        for (int c=0; c < colorSizes.length; ++c) {
            if (colorSizes[c]>0)
                indizesPerColor[c] = new int[colorSizes[c]];
        }

        int k = 0;
        for (Loss l : losses) {
            final int C = l.getTarget().getColor();
            indizesPerColor[C][--colorSizes[C]] = k;
            ++k;
        }
        for (int i = 0; i < indizesPerColor.length; ++i) {
            if (indizesPerColor[i]!=null) {
                final double[] ones = new double[indizesPerColor[i].length];
                Arrays.fill(ones, 1d);
                constraints.addRow(0, 1, indizesPerColor[i], ones);//String.valueOf(++identifier));
            }
        }
    }

    @Override
    protected void setMinimalTreeSizeConstraint() throws Exception {

        final int[] subroots = new int[graph.getRoot().getOutDegree()];
        final double[] ones = new double[subroots.length];
        Arrays.fill(ones, 1d);
        int from = edgeOffsets[graph.getRoot().getVertexId()];
        int k=0;
        for (int i=from, n = from+subroots.length; i < n; ++i) {
            subroots[k++] = edgeIds[i];
        }
        constraints.addRow(1, Double.POSITIVE_INFINITY, subroots, ones);

    }

    @Override
    protected void setObjective() throws Exception {
        final double[] weights = new double[losses.size()];
        for (int i=0; i < weights.length; ++i) {
            weights[i] = losses.get(i).getWeight();
        }
        model.addObjective(IloObjectiveSense.Maximize, model.scalProd(variables, weights));
    }

    @Override
    protected TreeBuilder.AbortReason solveMIP() throws Exception {
        model.setParam(IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Auto);
        model.setParam(IloCplex.IntParam.NodeAlg, IloCplex.Algorithm.Auto);
        final boolean done = model.solve();
        final IloCplex.Status status = model.getStatus();
        if (done && status == IloCplex.Status.Optimal)
            return TreeBuilder.AbortReason.COMPUTATION_CORRECT;
        if (status == IloCplex.Status.Infeasible)
            return TreeBuilder.AbortReason.INFEASIBLE;
        // probably timeout occurs?
        return TreeBuilder.AbortReason.TIMEOUT;

    }

    @Override
    protected void pastBuildSolution() throws Exception {
        model.endModel();
        model.end();
    }

    @Override
    protected boolean[] getVariableAssignment() throws Exception {
        final double[] weights = model.getValues(variables);
        final boolean[] assigned = new boolean[weights.length];
        for (int i=0; i < weights.length; ++i) {
            assigned[i] = weights[i] > 0.5d;
        }
        return assigned;
    }

    @Override
    protected double getSolverScore() throws Exception {
        return model.getObjValue();
    }
}
