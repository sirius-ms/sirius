package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import gurobi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class PCSTFragmentationTreeAnnotator extends AbstractFragmentationTreeAnnotator{

    private final CombinatorialFragmenter.Callback2 furtherFragmentation;
    private boolean isComputed;
    private double score;
    private int[] optSubtree;
    private HashMap<CombinatorialEdge, Integer> edgeIndices;
    private CombinatorialGraph graph;


    public PCSTFragmentationTreeAnnotator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring, CombinatorialFragmenter.Callback2 furtherFragmentation){
        super(fTree, molecule, scoring);
        this.furtherFragmentation = furtherFragmentation;
        this.isComputed = false;
        this.score = Double.NEGATIVE_INFINITY;
    }

    /* Now: we want to compute the most profitable rooted subtree in this graph.
     * This subtree is also known as the price-collecting steiner tree.
     * Usually, the fragments have assigned a profit/score and the edges are weighted with costs.
     * Thus, the profit of a subtree is defined as the sum of fragment profits minus the sum of edge costs.
     *
     * In this implementation, each CombinatorialEdge is also assigned a score and each CombinatorialNode
     * has a score and a fragment score. The nodes score is the sum of its fragment score plus the score
     * of an edge which belongs to the most profitable path to root.
     *
     * To make costs out of these edge scores, you have to give each edge a negative score/negative profit.
     * Then, the profit of the subtree is the sum of its fragment scores plus the sum of its edge scores.
     */

    @Override
    public HashMap<Fragment, ArrayList<CombinatorialFragment>> computeMapping() throws GRBException{
        // 1.: Creation of the combinatorial fragmentation graph:
        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.molecule, this.scoring);
        this.graph = fragmenter.createCombinatorialFragmentationGraph(this.furtherFragmentation);

        // 2.: Computing the most profitable subtree in this graph using an ILP solver.
        // For each edge there exists a binary variable which tells if the edge is part of the subtree.

        // 2.1.: Compute a hash map which maps each edge of the graph to its index in the variable array.
        this.edgeIndices = new HashMap<>();
        int idx = 0;
        for(CombinatorialNode node : this.graph.getNodes()){
            for(CombinatorialEdge edge : node.incomingEdges){
                this.edgeIndices.put(edge, idx);
                idx++;
            }
        }
        // idx is now equal to the number of edges - instead we use edge.Indices.keys() for readability
        // 2.2: Construct a ILP solver:
        this.optSubtree = this.createAndSolveILP();

        // 3.: Calculate the mapping with the computed solution of the ILP.
        this.mapping = this.calculateMapping();

        this.isComputed = true;
        return this.mapping;
    }

    private int[] createAndSolveILP() throws GRBException{
        // Initialisation of the gurobi environment:
        GRBEnv env = new GRBEnv(true);
        env.set("logFile", "pcst.log");
        env.start();

        // Create the ILP Model:
        GRBModel model = new GRBModel(env);

        // Add all variables and the objecive function to this model:
        GRBVar[] vars = new GRBVar[this.edgeIndices.size()];
        GRBLinExpr objFunc = new GRBLinExpr();

        for(CombinatorialEdge edge : this.edgeIndices.keySet()){
            int idx = this.edgeIndices.get(edge);
            double objCoeff = edge.target.fragmentScore + edge.score;
            vars[idx] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x"+idx);
            objFunc.addTerm(objCoeff, vars[idx]);
        }
        model.setObjective(objFunc, GRB.MAXIMIZE);

        // Add all constraints to the model:
        // Constraint 1: the number of incoming edges is at most one
        int i = 0;
        for(CombinatorialNode v : this.graph.getNodes()){ // every node except the root
            GRBLinExpr expr = new GRBLinExpr();
            for(CombinatorialEdge uv : v.incomingEdges){
                int idx = this.edgeIndices.get(uv);
                expr.addTerm(1, vars[idx]);
            }
            model.addConstr(expr, GRB.LESS_EQUAL, 1, "c1_"+i);
            i++;
        }

        // Constraint 2: connectivity inequalities
        i = 0;
        for(CombinatorialNode v : this.graph.getNodes()){
            for(CombinatorialEdge vw : v.outgoingEdges){
                int idx = this.edgeIndices.get(vw);
                GRBLinExpr expr = new GRBLinExpr();
                for(CombinatorialEdge uv : v.incomingEdges){
                    int idx2 = this.edgeIndices.get(uv);
                    expr.addTerm(1, vars[idx2]);
                }
                model.addConstr(expr,GRB.LESS_EQUAL,vars[idx], "c2_"+i);
                i++;
            }
        }

        // Optimize and get the optimal objective value and solution:
        model.optimize();
        this.score = model.get(GRB.DoubleAttr.ObjVal);
        double[] solution = model.get(GRB.DoubleAttr.X, vars);

        // Dispose the gurobi model and environment
        model.dispose();
        env.dispose();

        // transform the double array into an integer array
        int[] intSolution = new int[solution.length];
        for(int j = 0; j < solution.length; j++){
            intSolution[j] = (solution[j] == 1) ? 1 : 0;
        }
        return intSolution;
    }

    private HashMap<Fragment, ArrayList<CombinatorialFragment>> calculateMapping(){
        // Construct a hash map which maps each molecular formula (as string) to the respective FT-node.
        // And:
        // Initialize the hash map 'mapping' which maps each FT-fragment to the list of CombinatorialFragments, which
        // have the same molecular formula as the fragment and are contained in the prize-collecting steiner tree.
        HashMap<String, Fragment> mfToFtFrag = new HashMap<>();
        HashMap<Fragment, ArrayList<CombinatorialFragment>> mapping = new HashMap<>();
        for (Fragment fragment : this.fTree) {
            mfToFtFrag.put(fragment.getFormula().toString(), fragment);
            mapping.put(fragment, new ArrayList<>());
        }

        // Add the CombinatorialFragments of the solution to the respective lists:
        for(CombinatorialEdge edge : this.edgeIndices.keySet()){
            int idx = this.edgeIndices.get(edge);
            if(this.optSubtree[idx] == 1){
                Fragment fragment = mfToFtFrag.get(edge.target.fragment.getFormula().toString());
                if(fragment != null){
                    mapping.get(fragment).add(edge.target.fragment);
                }
            }
        }

        return mapping;
    }

    public double getScoreOfSolution(){
        return this.score;
    }

    public boolean isComputed(){
        return this.isComputed;
    }

    public HashMap<Fragment, ArrayList<CombinatorialFragment>> getMapping(){
        if(isComputed){
            return this.mapping;
        }else{
            return null;
        }
    }
}
