package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;

public class InsertionSubtreeCalculator extends CombinatorialSubtreeCalculator{

    private TObjectIntHashMap<CombinatorialNode> nodeIndices;
    private double[] in;
    private double[] out;
    private boolean isInitialised, isComputed;

    public InsertionSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        super(fTree, molecule, scoring);
        this.isInitialised = false;
        this.isComputed = false;
    }

    public InsertionSubtreeCalculator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring){
        super(fTree, graph, scoring);
        this.isInitialised = false;
        this.isComputed = false;
    }

    public void initialize(CombinatorialFragmenter.Callback2 fragmentationConstraint){
        if(this.isInitialised) throw new IllegalStateException("This object is already initialised.");

        // 1. Create the combinatorial fragmentation graph, if it hasn't been constructed yet:
        if(this.graph == null){
            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.molecule, this.scoring);
            this.graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
            CombinatorialGraphManipulator.addTerminalNodes(this.graph, this.scoring, this.fTree);
        }

        // 2. Assign each vertex in this.graph an individual index:
        int index = 0;
        this.nodeIndices = new TObjectIntHashMap<>(this.graph.numberOfNodes()-1);
        for(CombinatorialNode node : this.graph.getNodes()){
            this.nodeIndices.put(node, index);
            index++;
        }

        // 3. Initialise the arrays 'in' and 'out':
        this.in = new double[index];
        this.out = new double[index];
        Arrays.fill(this.in, Double.NEGATIVE_INFINITY);
        Arrays.fill(this.out, 0.0);

        // The initial subtree contains only the root.
        // Thus, initialise in(v) for each v contained in Adj[root].
        for(CombinatorialEdge e : this.graph.root.outgoingEdges){
            CombinatorialNode child = e.target;
            int idx = this.nodeIndices.get(child);
            this.in[idx] = e.score + child.fragmentScore;
        }

        this.isInitialised = true;
    }

    @Override
    public CombinatorialSubtree computeSubtree(){
        if(this.isComputed) return this.subtree;

        while(true){
            // 1. Find the vertex which maximizes in[v]+out[v] and is not contained in the subtree:
            double maxScore = Double.NEGATIVE_INFINITY;
            CombinatorialNode bestNode = null;
            for(CombinatorialNode node : this.graph.getNodes()){
                if(!this.subtree.contains(node.fragment)){
                    int idx = this.nodeIndices.get(node);
                    double score = this.in[idx] + this.out[idx];
                    if(score > maxScore){
                        maxScore = score;
                        bestNode = node;
                    }
                }
            }
            /* whether all nodes are already in the subtree or
             * the remaining nodes are connected by edges with score neg. infinity */
            if(bestNode == null) break;

            // In this case, bestNode != null.
            // 2. Find the optimal parent of bestNode and insert this node into the current subtree.
            // After that, update 'in' and 'out' scores.
            this.insertNode(bestNode);

            // 3. Search for possible relocations of edges in the current subtree
            // and update the 'out' scores:
            this.relocateAndUpdate(bestNode);
        }
        this.subtree.update();

        this.score = CombinatorialSubtreeManipulator.removeDanglingSubtrees(this.subtree);
        this.isComputed = true;
        return this.subtree;
    }

    private void relocateAndUpdate(CombinatorialNode v){ //'v' is the newly inserted node
        for(CombinatorialEdge vx : v.outgoingEdges){
            CombinatorialNode x = vx.target;
            if(this.subtree.contains(x.fragment)){
                double inEdgeOfXScore = this.subtree.getNode(x.fragment.bitset).incomingEdges.get(0).score;
                if(vx.score > inEdgeOfXScore){
                    // in this case, a rerouting increases the score of the subtree:
                    this.subtree.replaceSubtreeWithoutUpdate(v.fragment,x.fragment, vx.cut1, vx.cut2, vx.score);

                    // after rerouting, the 'out' scores have to be updated:
                    for(CombinatorialEdge ux : x.incomingEdges){
                        CombinatorialNode u = ux.source;
                        if(!this.subtree.contains(u.fragment)){
                            int uIdx = this.nodeIndices.get(u);
                            this.out[uIdx] = this.out[uIdx] - Math.max(0,ux.score - inEdgeOfXScore) +
                                    Math.max(0, ux.score - vx.score);
                        }
                    }
                }
            }
        }
    }

    private void insertNode(CombinatorialNode node){
        // 1. Find its optimal parent in the tree:
        int nodeIdx = this.nodeIndices.get(node);
        CombinatorialEdge optEdge = null;
        for(CombinatorialEdge edge : node.incomingEdges){
            CombinatorialNode parent = edge.source;
            if(this.subtree.contains(parent.fragment)){
                double score = edge.score + node.fragmentScore;
                if(score == this.in[nodeIdx]){
                    optEdge = edge;
                    break;
                }
            }
        }

        // 2. Attach 'node' to its optimal parent in the tree:
        this.subtree.addFragment(this.subtree.getNode(optEdge.source.fragment.bitset), node.fragment,
                optEdge.cut1, optEdge.cut2, node.fragmentScore, optEdge.score);

        // 3. Update 'out' scores:
        for(CombinatorialEdge edge : node.incomingEdges){
            CombinatorialNode parent = edge.source;
            if(!this.subtree.contains(parent.fragment)){
                int parentIdx = this.nodeIndices.get(parent);
                this.out[parentIdx] = this.out[parentIdx] + Math.max(0, edge.score - optEdge.score);
            }
        }

        // 4. Update 'in' scores:
        for(CombinatorialEdge edge : node.outgoingEdges){
            CombinatorialNode child = edge.target;
            if(!this.subtree.contains(child.fragment)){
                int childIdx = this.nodeIndices.get(child);
                this.in[childIdx] = Math.max(this.in[childIdx], edge.score + child.fragmentScore);
            }
        }
    }

    public boolean isInitialised(){
        return this.isInitialised;
    }

    public boolean isComputed(){
        return this.isComputed;
    }

    @Override
    public String getMethodName(){
        return "Insertion";
    }
}
