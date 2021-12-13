package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import gurobi.*;

import java.util.BitSet;
import java.util.HashMap;

public class PCSTFragmentationTreeAnnotator extends CombinatorialSubtreeCalculator{

    private CombinatorialGraph graph;

    private HashMap<CombinatorialEdge, Integer> edgeIndices;
    private boolean isComputed;
    private boolean isInitialized;


    public PCSTFragmentationTreeAnnotator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        super(fTree, molecule, scoring);
        this.isComputed = false;
        this.isInitialized = false;
    }

    public void initialize(CombinatorialFragmenter.Callback2 furtherFragmentation){
        if(this.isInitialized) throw new IllegalStateException("This object is already initialized.");

        // 1.: Creation of the combinatorial fragmentation graph:
        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.molecule, this.scoring);
        this.graph = fragmenter.createCombinatorialFragmentationGraph(furtherFragmentation);

        // 2.: Compute a hash map which assigns each edge in the graph an index.
        this.edgeIndices = new HashMap<>();
        int idx = 0;
        for(CombinatorialNode node : this.graph.getNodes()){
            for(CombinatorialEdge edge : node.incomingEdges){
                this.edgeIndices.put(edge, idx);
                idx++;
            }
        }
        this.isInitialized = true;
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
    public CombinatorialSubtree computeSubtree() throws GRBException{
        // During the initialisation the input molecule was fragmented and a CombinatorialFragmentationGraph was
        // constructed. Also a mapping was created which assigns each edge of this graph a specific position
        // in the variable array.
        // 1.: Construct an ILP solver to solve the PCSTP:
        double[] solution = this.createAndSolveILP();

        // 2.: Build the subtree with the computed solution of the ILP.
        this.buildSubtree(solution, this.graph.root);

        this.isComputed = true;

        return this.subtree;
    }

    private double[] createAndSolveILP() throws GRBException{
        // Initialisation of the gurobi environment:
        GRBEnv env = new GRBEnv(true);
        env.start();

        // Create the ILP Model:
        GRBModel model = new GRBModel(env);

        // Add all variables and the objective function to this model:
        GRBVar[] vars = new GRBVar[this.edgeIndices.size()];
        GRBLinExpr objFunc = new GRBLinExpr();

        for(CombinatorialEdge edge : this.edgeIndices.keySet()){
            int idx = this.edgeIndices.get(edge);
            double objCoeff = edge.target.fragmentScore + edge.score;
            vars[idx] = model.addVar(0.0, 1.0, objCoeff, GRB.BINARY, "x"+idx);
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
                model.addConstr(expr,GRB.GREATER_EQUAL,vars[idx], "c2_"+i);
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

        return solution;
    }

    // rekursive Methode: diese Methode traversiert den CombinatorialGraph und
    // betrachtet dabei nur Kanten, die in solution den Wert Eins haben
    // Achtung: currentNode ist Zeiger auf den momentan Knoten IM COMBINATORIALGRAPH!
    private void buildSubtree(double[] solution, CombinatorialNode currentNode){
        for(CombinatorialEdge edge : currentNode.outgoingEdges){
            int idx = this.edgeIndices.get(edge);
            if(solution[idx] == 1){
                BitSet currentNodeBitSet = currentNode.fragment.bitset;
                CombinatorialNode subtreeNode = this.subtree.getNode(currentNodeBitSet);
                this.subtree.addFragment(subtreeNode, edge.target.fragment, edge.cut1, edge.cut2, edge.target.fragmentScore, edge.score);

                CombinatorialNode nextNode = edge.target;
                buildSubtree(solution, nextNode);
            }
        }
    }

    public boolean isComputed(){
        return this.isComputed;
    }

}
