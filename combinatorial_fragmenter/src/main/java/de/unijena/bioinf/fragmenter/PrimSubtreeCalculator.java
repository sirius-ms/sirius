package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;
import java.util.PriorityQueue;

public class PrimSubtreeCalculator extends CombinatorialSubtreeCalculator {

    private TObjectIntHashMap<CombinatorialNode> nodeIndices;
    private double[] nodeScores;
    private boolean isInitialized, isComputed;

    public PrimSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        super(fTree, molecule, scoring);
        this.isInitialized = false;
        this.isComputed = false;
    }

    public PrimSubtreeCalculator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring){
        super(fTree, graph, scoring);
        this.isInitialized = false;
        this.isComputed = false;
    }

    public void initialize(CombinatorialFragmenter.Callback2 fragmentationConstraint){
        if(this.isInitialized) throw new IllegalStateException("This object has been already initialised.");

        // 1.) Create fragmentation graph and add the terminal nodes - if it hasn't been done before:
        if(this.graph == null) {
            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.molecule, scoring);
            this.graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
            CombinatorialGraphManipulator.addTerminalNodes(this.graph, this.scoring, this.fTree);
        }

        // 2.) Assign each node of this graph - except the root - a unique index:
        this.nodeIndices = new TObjectIntHashMap<>(this.graph.nodes.size());
        int idx = 0;
        for(CombinatorialNode node : this.graph.nodes){
            this.nodeIndices.put(node,idx);
            idx++;
        }

        // 3.) Initialise the array 'nodesScores'.
        // This array contains for each node the sum of edge and fragment score of the most profitable edge which
        // connects this node with the current computed subtree.
        // If there is not such an edge, this value is set to negative infinity.
        this.nodeScores = new double[this.graph.nodes.size()];
        Arrays.fill(this.nodeScores, Double.NEGATIVE_INFINITY);
        for(CombinatorialEdge edge : this.graph.root.outgoingEdges){
            CombinatorialNode child = edge.target;
            idx = this.nodeIndices.get(child);
            this.nodeScores[idx] = edge.score + child.fragmentScore;
        }

        this.isInitialized = true;
    }

    @Override
    public CombinatorialSubtree computeSubtree(){
        if(this.isComputed) return this.subtree;

        // 1. Initialize the PriorityQueue (aka binary heap):
        PriorityQueue<CombinatorialNode> heap = new PriorityQueue<CombinatorialNode>(this.graph.nodes.size(), (n1,n2) -> {
            double score1 = this.nodeScores[this.nodeIndices.get(n1)];
            double score2 = this.nodeScores[this.nodeIndices.get(n2)];
            return (int) Math.signum(score2 - score1);
        });
        heap.addAll(this.graph.nodes);

        // 2. In each iteration step, the node on top of the heap is removed and added with its best edge to the subtree.
        // This is done until the heap is empty and all nodes of the fragmentation graph are added to the subtree.
        while(heap.size() > 0){
            CombinatorialNode bestNode = heap.poll();
            this.addBestNodeAndEdge(bestNode, heap);
            this.updateNodeScoresAndHeap(bestNode, heap);
        }
        this.score = CombinatorialSubtreeManipulator.removeDanglingSubtrees(this.subtree);;
        this.isComputed = true;

        return this.subtree;
    }

    private void addBestNodeAndEdge(CombinatorialNode bestNode, PriorityQueue<CombinatorialNode> heap){
        double bestScore = this.nodeScores[this.nodeIndices.get(bestNode)];

        CombinatorialEdge bestEdge = null;
        for(CombinatorialEdge e : bestNode.incomingEdges){
            if(this.subtree.contains(e.source.fragment)){
                double score = e.score + bestNode.fragmentScore;
                if(bestScore == score){
                    bestEdge = e;
                    break;
                }
            }
        }
        this.subtree.addFragment(this.subtree.getNode(bestEdge.source.fragment.bitset),
                            bestNode.fragment, bestEdge.cut1, bestEdge.cut2, bestNode.fragmentScore, bestEdge.score);
    }

    private void updateNodeScoresAndHeap(CombinatorialNode node, PriorityQueue<CombinatorialNode> heap){
        for(CombinatorialEdge edge : node.outgoingEdges){
            CombinatorialNode child = edge.target;
            if(!this.subtree.contains(child.fragment)){
                int idx = this.nodeIndices.get(child);
                this.nodeScores[idx] = Math.max(this.nodeScores[idx], edge.score + child.fragmentScore);
                heap.remove(child); heap.add(child);
            }
        }
    }

    @Override
    public CombinatorialGraph getCombinatorialGraph(){
        return this.graph;
    }

    public boolean isInitialized(){
        return this.isInitialized;
    }

    public boolean isComputed(){
        return this.isComputed;
    }

    @Override
    public String getMethodName(){
        return "Prim";
    }
}
