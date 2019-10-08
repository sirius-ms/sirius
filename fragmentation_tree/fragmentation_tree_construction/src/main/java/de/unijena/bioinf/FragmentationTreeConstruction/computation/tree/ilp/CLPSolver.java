package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLPSolver extends AbstractSolver{

    public final static IlpFactory<CLPSolver> Factory = new IlpFactory<CLPSolver>() {
        @Override
        public CLPSolver create(ProcessedInput input, FGraph graph, TreeBuilder.FluentInterface options) {
            return new CLPSolver(graph,input,options);
        }

        @Override
        public boolean isThreadSafe() {
            return true;
        }

        @Override
        public String name() {
            return "COIN-OR LP";
        }
    };

    protected CLPModel model;

    public CLPSolver(FGraph graph, ProcessedInput input, TreeBuilder.FluentInterface options) {
        super(graph, input, options);
    }

    @Override
    protected void setTimeLimitInSeconds(double timeLimitsInSeconds) throws Exception {
        // TODO: is this possible?
    }

    @Override
    protected void setNumberOfCpus(int numberOfCPUS) throws Exception {
        // TODO: is this possible?
    }

    @Override
    protected void initializeModel() throws Exception {
        this.model = new CLPModel(losses.size(), CLPModel.ObjectiveSense.MAXIMIZE);
    }

    @Override
    protected void setMinimalScoreConstraints(double minimalScore) throws Exception {
        // TODO: is this possible?
    }

    @Override
    protected void defineVariables() throws Exception {
        final double[] lb = new double[losses.size()];
        final double[] ub = new double[losses.size()];
        final double[] objective = new double[losses.size()];
        for (int i = 0; i < losses.size(); ++i) {
            lb[i] = 0d;
            ub[i] = 1d;
            objective[i] = losses.get(i).getWeight();
        }
        model.setColBounds(lb, ub);
        model.setObjective(objective);
    }

    @Override
    protected void setVariableStartValues(int[] usedEdgeIds) throws Exception {
        final double[] values = new double[losses.size()];
        for (int index : usedEdgeIds) values[index] = 1d;
        model.setColStart(values);
    }

    @Override
    protected void setTreeConstraint() throws Exception {
        int k = 0;
        for (Fragment fragment : graph.getFragmentsWithoutRoot()) {
            final int[] indices = new int[fragment.getInDegree()];
            final double[] elems = new double[fragment.getInDegree()];
            Arrays.fill(elems, 1d);
            for (int l = 0; l < fragment.getInDegree(); ++l)
                indices[l] = k++;
            model.addSparseRow(elems, indices, 0d, 1d);
            // 1. For all vertices u with u!=root the sum of all edges uv for a fixed v is <= 1
            // => TreeCondition: Each vertex (except root has a parent
            // 2. An edge is only set, if one of the incomming edges of its head is set
            // => Connectivity-Condition: There is a path between each two vertices
            int j = edgeOffsets[fragment.getVertexId()];
            final int[] indices2 = Arrays.copyOf(indices, indices.length+1);
            final double[] elems2 = Arrays.copyOf(elems, elems.length+1);
            final int n = indices.length;
            elems2[n] = -1d;
            for (int l = 0; l < fragment.getOutDegree(); ++l) {
                elems2[n] = edgeIds[j];
                model.addSparseRow(elems2, indices2, 0d, 1d);
                assert losses.get(edgeIds[j]).getSource() == fragment;
                ++j;
            }
        }
    }

    @Override
    protected void setColorConstraint() throws Exception {
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
                model.addSparseRow(ones, indizesPerColor[i], 0, 1);
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
        model.addSparseRow(ones, subroots, 1d, model.getInfinity());
    }

    @Override
    protected void setObjective() throws Exception {
        // already done
    }

    protected static final Logger logger = LoggerFactory.getLogger(GrbSolver.class);

    @Override
    protected TreeBuilder.AbortReason solveMIP() throws Exception {
        int return_status = model.solve();
        // TODO: how to handle timeouts, score cutoff
        switch (return_status){
            case CLPModel.ReturnStatus.OPTIMAL:
                return TreeBuilder.AbortReason.COMPUTATION_CORRECT;
            case CLPModel.ReturnStatus.INFEASIBLE:
                logger.info("Solution is infeasible");
                return TreeBuilder.AbortReason.INFEASIBLE;
            case CLPModel.ReturnStatus.ABANDONED:
                logger.info("Model was abandoned");
            case CLPModel.ReturnStatus.LIMIT_REACHED:
                logger.info("Objective and/or iteration limits were reached");
            default:
                return TreeBuilder.AbortReason.NO_SOLUTION;
        }
    }

    @Override
    protected void pastBuildSolution() throws Exception {
        model.dispose();
    }

    @Override
    protected boolean[] getVariableAssignment() throws Exception {
        final double[] weights = model.getColSolution();
        final boolean[] assigned = new boolean[weights.length];
        for (int i=0; i < weights.length; ++i) {
            assigned[i] = weights[i] > 0.5d;
        }
        return assigned;
    }

    @Override
    protected double getSolverScore() throws Exception {
        return model.getScore();
    }
}
