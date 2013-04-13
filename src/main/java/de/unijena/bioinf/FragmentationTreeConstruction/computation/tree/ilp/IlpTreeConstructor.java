package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.functional.iterator.Iterators;
import net.sf.javailp.*;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class IlpTreeConstructor implements TreeBuilder {

    private final SolverFactory factory;

    public IlpTreeConstructor(SolverFactory factory) {
        this.factory = factory;
        factory.setParameter(Solver.VERBOSE, 1);
    }

    public IlpTreeConstructor() {
        this(new SolverFactoryGurobi());
    }


    @Override
    public FragmentationTree buildTree(ProcessedInput input, FragmentationGraph graph, double upperbound) {
        final Result solution = factory.get().solve(computeTreeMS2Mode(graph));
        final double solutionScore;
        if (Double.isNaN(solution.getObjective().doubleValue())) {
            double s = 0d;
            for (Loss l : Iterators.asIterable(graph.lossIterator())) {
                if (solution.get(l).intValue() > 0) {
                    s += l.getWeight();
                }
            }
            solutionScore = s + graph.getRootScore();
        } else {
            solutionScore = solution.getObjective().doubleValue() + graph.getRootScore();
        }
        final FragmentationTree tree = new FragmentationTree(solutionScore, graph);
        final ArrayDeque<Stackitem> stack = new ArrayDeque<Stackitem>();
        stack.push(new Stackitem(tree.getRoot(), graph.getRoot()));
        while (!stack.isEmpty()) {
            final Stackitem item = stack.pop();
            for (Loss l : item.graphNode.getOutgoingEdges()) {
                if (Math.round(solution.get(l).intValue()) > 0) {
                    stack.push(new Stackitem(tree.addVertex(item.treeNode, l), (GraphFragment)l.getTail()));
                }
            }
        }
        assert tree.isComputationCorrect();
        return tree;
    }

    protected static class Stackitem {
        private final TreeFragment treeNode;
        private final GraphFragment graphNode;

        protected Stackitem(TreeFragment treeNode, GraphFragment graphNode) {
            this.treeNode = treeNode;
            this.graphNode = graphNode;
        }
    }

    protected Problem computeTreeMS2Mode(FragmentationGraph graph){

        final Problem problem = new Problem();
        int counter = 0;

        //objective function
        final Linear maximizeWeight = new Linear();
        for(Loss s : Iterators.asIterable(graph.lossIterator())){
            maximizeWeight.add(s.getWeight(),s);
        }
        problem.setObjective(maximizeWeight,OptType.MAX);

        final GraphFragment root = graph.getRoot();
        //Constraints
        for(GraphFragment v : graph.getFragments()) {
            final Linear treeConstraint = new Linear();
            for(Loss uv : v.getIncomingEdges()){
                treeConstraint.add(1,uv);
            }
            problem.add(String.valueOf(++counter), treeConstraint, "<=", 1); //Baum erwuenscht -> Indegree <= 1

            if(v!=root){
                for(Loss vw : v.getOutgoingEdges()){
                    final Linear connectedConstraint = new Linear(treeConstraint);
                    connectedConstraint.add(-1,vw);
                    problem.add(String.valueOf(++counter), connectedConstraint, ">=",0); //verbundenen Graphen erwuenscht
                }
            }
        }

        final ArrayList<ArrayList<GraphFragment>> verticesPerColor = graph.verticesPerColor();

        for(int i=0;i<verticesPerColor.size(); ++i) {
            final ArrayList<GraphFragment> vertices = verticesPerColor.get(i);
            if (vertices.size() <= 1) continue;
            final Linear colorConstraint = new Linear();
            for(GraphFragment v : vertices){
				for(Loss uv : v.getIncomingEdges()){
                    colorConstraint.add(1,uv);
                }
            }
            problem.add(String.valueOf(++counter), colorConstraint, "<=", 1); //immer nur einen Vertex mit
        }

        for(Loss l : Iterators.asIterable(graph.lossIterator())){
            problem.setVarType(l, Boolean.class);
        }

        return problem;
    }
}
