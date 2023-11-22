package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.CLPModel_JNI;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

public class PCSTFragmentationTreeAnnotator extends CombinatorialSubtreeCalculator{

    private HashMap<CombinatorialEdge, Integer> edgeIndices;
    private boolean isComputed;
    private boolean isInitialized;


    public PCSTFragmentationTreeAnnotator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        super(fTree, molecule, scoring);
        this.isComputed = false;
        this.isInitialized = false;
    }

    public PCSTFragmentationTreeAnnotator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring){
        super(fTree, graph, scoring);
        this.isComputed = false;
        this.isInitialized = false;
    }

    public void initialize(CombinatorialFragmenter.Callback2 fragmentationConstraint){
        if(this.isInitialized) throw new IllegalStateException("This object is already initialized.");

        // 1.: Creation of the combinatorial fragmentation graph - if it hasn't been computed yet:
        if(this.graph == null) {
            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.molecule, this.scoring);
            this.graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
            CombinatorialGraphManipulator.addTerminalNodes(this.graph, this.scoring, this.fTree);
        }

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
    public CombinatorialSubtree computeSubtree() {
        if(this.isComputed) return this.subtree;
        // During the initialisation the input molecule was fragmented and a CombinatorialFragmentationGraph was
        // constructed. Also, a mapping was created which assigns each edge of this graph a specific position
        // in the variable array.
        // 1.: Construct an ILP solver to solve the PCSTP:
        double[] solution = this.createAndSolveILP();

        // 2.: Build the subtree with the computed solution of the ILP.
        this.buildSubtree(solution, this.graph.root);

        this.isComputed = true;

        return this.subtree;
    }

    private double[] createAndSolveILP() {
        // Initialisation of the CBC solver:
        final CLPModel_JNI model = new CLPModel_JNI(this.edgeIndices.size(), CLPModel_JNI.ObjectiveSense.MAXIMIZE);

        // Add all variable boundaries and the objective function to this model:
        final double[] lb = new double[this.edgeIndices.size()];
        final double[] ub = new double[this.edgeIndices.size()];
        final double[] objective = new double[this.edgeIndices.size()];
        for(final CombinatorialEdge edge : this.edgeIndices.keySet()){
            final int edgeIdx = this.edgeIndices.get(edge);
            lb[edgeIdx] = 0d;
            ub[edgeIdx] = 1d;
            objective[edgeIdx] = edge.score + edge.target.fragmentScore;
        }
        model.setColBounds(lb, ub);
        model.setObjective(objective);

        // Add all constraints to the model:
        // Constraint 1: the number of incoming edges is at most one
        for(final CombinatorialNode v : this.graph.getNodes()){
            final int[] indices = new int[v.incomingEdges.size()];
            final double[] coeffs = new double[v.incomingEdges.size()];
            Arrays.fill(coeffs, 1d);
            for(int i = 0; i < v.incomingEdges.size(); i++){
                final CombinatorialEdge uv = v.incomingEdges.get(i);
                indices[i] = this.edgeIndices.get(uv);
            }
            model.addSparseRowCached(coeffs, indices, 0d, 1d);
        }

        // Constraint 2: connectivity inequalities
        for(final CombinatorialNode v : this.graph.getNodes()){
            for(final CombinatorialEdge vw : v.outgoingEdges){
                final int[] indices = new int[v.incomingEdges.size()+1];
                final double[] coeffs = new double[v.incomingEdges.size()+1];
                for(int i = 0; i < v.incomingEdges.size(); i++){
                    final CombinatorialEdge uv = v.incomingEdges.get(i);
                    indices[i] = this.edgeIndices.get(uv);
                    coeffs[i] = 1d;
                }
                indices[v.incomingEdges.size()] = this.edgeIndices.get(vw);
                coeffs[v.incomingEdges.size()] = -1d;
                model.addSparseRowCached(coeffs, indices, 0d, 1d);
            }
        }

        // Optimize and get the optimal objective value and solution:
        model.solve();
        this.score = model.getScore();
        double[] solution = model.getColSolution();
        for(int i = 0; i < solution.length; i++) solution[i] = solution[i] > 0.5d ? 1d : 0d;

        // Past Build Solution:
        model.dispose();

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

    public boolean isInitialized(){
        return this.isInitialized;
    }

    public boolean isComputed(){
        return this.isComputed;
    }

    @Override
    public String getMethodName(){
        return "ILP";
    }

}
