package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CriticalPath 3
 */
public class CriticalPathInsertionSubtreeCalculator extends CombinatorialSubtreeCalculator{

    private TObjectIntHashMap<CombinatorialNode> nodeIndices;
    private ArrayList<CombinatorialEdge> selectableEdges;
    private double[] criticalPathScores, out;
    private boolean isInitialised, isComputed;

    public CriticalPathInsertionSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        super(fTree, molecule, scoring);
        this.isInitialised = false;
        this.isComputed = false;
    }

    public CriticalPathInsertionSubtreeCalculator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring){
        super(fTree, graph, scoring);
        this.isInitialised = false;
        this.isComputed = false;
    }

    public void initialize(CombinatorialFragmenter.Callback2 fragmentationConstraint){
        if(this.isInitialised) throw new IllegalStateException("This object has been already initialised.");

        // 1. Create the combinatorial fragmentation graph,
        // if this object was created by giving it only the molecule.
        if(this.graph == null){
            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.molecule, this.scoring);
            this.graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
            CombinatorialGraphManipulator.addTerminalNodes(this.graph, this.scoring, this.fTree);
        }

        // 2. Construct a hashmap which assigns each vertex in this.graph a unique index.
        this.nodeIndices = new TObjectIntHashMap<>(this.graph.numberOfNodes());
        this.nodeIndices.put(this.graph.getRoot(), 0);

        List<CombinatorialNode> nodes = this.graph.getNodes();
        for(int idx = 0; idx < nodes.size(); idx++){
            this.nodeIndices.put(nodes.get(idx), idx+1);
        }

        // 3. Construct the array 'criticalPathScores' and initialise it with NaN values:
        this.criticalPathScores = new double[this.graph.numberOfNodes()];
        Arrays.fill(this.criticalPathScores, Double.NaN);

        /* 4. Construct the array 'out' and fill it with 0.
         * Actually, the root doesn't need an out-score. But we want to simplify the implementation of this heuristic.
         * So, the size of the array 'out' is equal to the number of nodes found in this.graph.
         */
        this.out = new double[this.graph.numberOfNodes()];
        Arrays.fill(this.out, 0.0);

        // 5. Initialise the set of edges which can be added to the tree in the next iteration step.
        this.selectableEdges = new ArrayList<>(this.graph.getRoot().getOutgoingEdges());

        this.isInitialised = true;
    }

    @Override
    public CombinatorialSubtree computeSubtree(){
        if(this.isComputed) return this.subtree;

        // While there are edges left that can be added to the subtree, do the following:
        // - find the edge (u,v) in 'selectableEdges' that maximizes S[u] + I(u,v)
        // - add (u,v) to the subtree and do rerouting according to I(u,v) ('out[v]')
        // - reset S, update 'out' and 'selectableEdges'
        while(true) {
            // I.: Find the best edge with maximal score:
            double maxScore = Double.NEGATIVE_INFINITY;
            CombinatorialEdge bestEdge = null;
            for (CombinatorialEdge edge : this.selectableEdges) {
                int childIdx = this.nodeIndices.get(edge.target);
                double score = this.getCriticalPathScore(edge.source) +
                        edge.score + edge.target.fragmentScore + this.out[childIdx];
                if (score > maxScore) {
                    maxScore = score;
                    bestEdge = edge;
                }
            }
            if (bestEdge == null) break;

            // II.: Add 'bestEdge' to the subtree and update the corresponding 'out' scores:
            this.addEdgeAndUpdate(bestEdge);

            // III.: Reroute and update the corresponding 'out' scores:
            this.rerouteAndUpdate(bestEdge.target);

            // IV.: Reset 'criticalPathScores' and update 'selectableEdges':
            Arrays.fill(this.criticalPathScores, Double.NaN);
            updateSelectableEdges(bestEdge.target);
        }
        this.subtree.update();
        this.score = CombinatorialSubtreeManipulator.removeDanglingSubtrees(this.subtree);
        this.isComputed = true;
        return this.subtree;
    }

    public void updateSelectableEdges(CombinatorialNode newNode){
        // 1. Remove all incoming edges of 'newNode' in 'this.selectableEdges':
        for(CombinatorialEdge edge : newNode.getIncomingEdges()){
            CombinatorialNode parent = edge.source;
            if(this.subtree.contains(parent.fragment)) this.selectableEdges.remove(edge);
        }

        // 2. Add all outgoing edges of 'newNode' with a target, not contained in the subtree, to 'this.selectableEdges':
        for(CombinatorialEdge edge : newNode.getOutgoingEdges()){
            CombinatorialNode child = edge.target;
            if(!this.subtree.contains(child.fragment)) this.selectableEdges.add(edge);
        }
    }

    public void rerouteAndUpdate(CombinatorialNode v){
        for(CombinatorialEdge vx : v.getOutgoingEdges()){
            CombinatorialNode x = vx.target;
            if(this.subtree.contains(x.fragment)){
                float incomingEdgeOfXScore = this.subtree.getNode(x.fragment.bitset).getIncomingEdges().get(0).score;
                if(vx.score > incomingEdgeOfXScore){
                    // In this case, the profit of the subtree can be improved by
                    // deleting the old incoming edge of 'x' and inserting the edge 'vx'
                    this.subtree.replaceSubtreeWithoutUpdate(v.fragment,x.fragment,vx.cut1, vx.cut2, vx.score);

                    // Now, the parent of 'x' changed to 'v'.
                    // That means, that the 'out' score of each node 'w' with an edge to 'x' has to be updated.
                    for(CombinatorialEdge wx : x.getIncomingEdges()){
                        CombinatorialNode w = wx.source;
                        if(!this.subtree.contains(w.fragment)){
                            int idx = this.nodeIndices.get(w);
                            this.out[idx] = this.out[idx] - Math.max(0, wx.score - incomingEdgeOfXScore) +
                                    Math.max(0, wx.score - vx.score);
                        }
                    }
                }
            }
        }
    }

    private void addEdgeAndUpdate(CombinatorialEdge uv){
        // add the edge to the subtree:
        CombinatorialNode u = uv.source;
        CombinatorialNode v = uv.target;
        this.subtree.addFragment(this.subtree.getNode(u.fragment.bitset), v.fragment, uv.cut1, uv.cut2,
                v.fragmentScore, uv.score);

        // update the corresponding 'out' scores:
        for(CombinatorialEdge wv : v.getIncomingEdges()){
            CombinatorialNode w = wv.source;
            if(!this.subtree.contains(w.fragment)){
                int idx = this.nodeIndices.get(w);
                this.out[idx] = this.out[idx] + Math.max(0, wv.score - uv.score);
            }
        }
    }


    private double getCriticalPathScore(CombinatorialNode node){
        int idx = this.nodeIndices.get(node);
        if(!Double.isNaN(this.criticalPathScores[idx])) return this.criticalPathScores[idx];

        double maxScore = 0.0;
        for(CombinatorialEdge edge : node.getOutgoingEdges()){
            CombinatorialNode child = edge.target;
            if(!this.subtree.contains(child.fragment)){
                double score = this.getCriticalPathScore(child) + child.fragmentScore + edge.score;
                if(score > maxScore){
                    maxScore = score;
                }
            }
        }
        return maxScore;
    }

    public boolean isComputed(){
        return this.isComputed;
    }

    public boolean isInitialised(){
        return this.isInitialised;
    }

    public String getMethodName(){
        return "CriticalPath3";
    }
}
