package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.List;

/**
 * Created by xentrics on 27.03.15.
 */
public class ilp_base_construct implements TreeBuilder {

    public ilp_base_construct() {

    }

    class Solver extends AbstractSolver {


        protected Solver(FGraph graph, ProcessedInput input, double lowerbound, TreeBuilder feasibleSolver, int timeLimit) {
            super(graph, input, lowerbound, feasibleSolver, timeLimit);
        }

        @Override
        protected void defineVariables() throws Exception {

        }

        @Override
        protected void defineVariablesWithStartValues(FTree presolvedTree) throws Exception {

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

        }

        @Override
        protected int solveMIP() throws Exception {
            return 0;
        }

        @Override
        protected int pastBuildSolution() throws Exception {
            return 0;
        }

        @Override
        protected boolean[] getVariableAssignment() throws Exception {
            return new boolean[0];
        }

        @Override
        protected double getSolverScore() throws Exception {
            return 0;
        }
    }

    @Override
    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return null;
    }

    @Override
    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation) {
        return null;
    }

    @Override
    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound) {
        return null;
    }
}
