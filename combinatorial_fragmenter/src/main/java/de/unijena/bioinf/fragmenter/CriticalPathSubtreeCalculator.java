package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

public class CriticalPathSubtreeCalculator extends CombinatorialSubtreeCalculator{

    public final static boolean ALLOW_MULTIPLE_FRAGMENTS_PER_PEAK = true;

    public final static int INSERTED = 5, ATTACHED_TO_TERMINAL=7, DELETED = 9;

    private boolean isInitialized, isComputed;
    private TObjectIntHashMap<CombinatorialNode> vertexIndices;
    private float[] criticalPathScores;
    private ArrayList<CombinatorialNode> insertedNodes = new ArrayList<>();
    protected int maxNumberOfNodes = Integer.MAX_VALUE;
    private final boolean addCompletePath;

    public CriticalPathSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring, boolean addCompletePath){
        super(fTree, molecule, scoring);
        this.isInitialized = false;
        this.isComputed = false;
        this.addCompletePath = addCompletePath;
    }

    public int getMaxNumberOfNodes() {
        return maxNumberOfNodes;
    }

    public void setMaxNumberOfNodes(int maxNumberOfNodes) {
        this.maxNumberOfNodes = maxNumberOfNodes;
    }

    public CriticalPathSubtreeCalculator(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring, boolean addCompletePath){
        super(fTree, graph, scoring);
        this.isInitialized = false;
        this.isComputed = false;
        this.addCompletePath = addCompletePath;
    }

    public void initialize(CombinatorialFragmenter.Callback2 fragmentationConstraint){
        if(this.isInitialized) throw new IllegalStateException("This object is already initialised.");

        // 1. Create a CombinatorialFragmentationGraph - if it hasn't been computed yet:
        if(this.graph == null) {
            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule, scoring);
            this.graph = fragmenter.createCombinatorialFragmentationGraphPriorized(fragmentationConstraint, maxNumberOfNodes);
            CombinatorialGraphManipulator.addTerminalNodes(this.graph, this.scoring, this.fTree);
        }

        // 2. Determine a hashmap which assigns each CombinatorialNode in this.graph a specific integer index.
        this.vertexIndices = new TObjectIntHashMap<>();
        this.vertexIndices.put(this.graph.getRoot(),0);

        List<CombinatorialNode> nodes = this.graph.getNodes(); // all nodes in this.graph except the root
        for(int idx = 0; idx < nodes.size(); idx++){
            this.vertexIndices.put(nodes.get(idx), idx+1);
        }

        // 3. Initialise the array this.criticalPathScores:
        this.criticalPathScores = new float[nodes.size() +1];
        Arrays.fill(this.criticalPathScores, Float.NaN);
        insertedNodes.clear();

        this.isInitialized = true;
    }

    @Override
    public CombinatorialSubtree computeSubtree(){
        if(this.isComputed) return this.subtree;
        /* At this moment:
         * - the initial subtree is already initialised --> it contains only the root
         * - the array of critical path scores is initialised with NaN values
         *  --> these scores have to be computed now by calling calculateCriticalPathScore(graph.root)*/
        this.calculateCriticalPathScore(this.graph.root);
        addFragment(this.graph.root,null);
        CombinatorialNode maxScoreNode = this.graph.root; //node in subtree with maximum critical path score
        int maxScoreNodeIdx = this.vertexIndices.get(maxScoreNode);

        /* In each iteration step:
         * - the critical path is attached to maxScoreNode in this.subtree
         * - the criticalPathScores are reset and calculated regarding to the new subtree
         * - then, the node in subtree is found with maximum critical path score
         */
        while(this.criticalPathScores[maxScoreNodeIdx] > 0){
            if(this.addCompletePath){
                this.addCriticalPathToTree(maxScoreNode);
                Arrays.fill(this.criticalPathScores, Float.NaN);
            }else{
                this.addCriticalEdgeToTree(maxScoreNode);
                Arrays.fill(this.criticalPathScores, Float.NaN);
            }

            float bestScore = Float.NEGATIVE_INFINITY;
            for(CombinatorialNode node : insertedNodes){
                float score = this.calculateCriticalPathScore(node);
                if(score > bestScore){
                    maxScoreNode = node;
                    bestScore = score;
                }
            }
            maxScoreNodeIdx = this.vertexIndices.get(maxScoreNode);
        }

        this.isComputed = true;
        this.score = this.subtree.getScore();
        return this.subtree;
    }

    public ArrayList<CombinatorialNode> getInsertedNodes() {
        return insertedNodes;
    }

    private float calculateCriticalPathScore(CombinatorialNode node) {
        int nodeIdx = this.vertexIndices.get(node);
        // check, if the CriticalPath score of node was calculated
        if(!Float.isNaN(this.criticalPathScores[nodeIdx])) return this.criticalPathScores[nodeIdx];

        float bestScore = 0f;
        for(CombinatorialEdge edge : node.outgoingEdges){
            CombinatorialNode child = edge.target;
            if(child.state<INSERTED) {//!this.subtree.contains(child.fragment)){
                float score = this.calculateCriticalPathScore(child) + child.fragmentScore + edge.score;
                // this prevents a node from explaining multiple peaks
                if (!ALLOW_MULTIPLE_FRAGMENTS_PER_PEAK && node.state==ATTACHED_TO_TERMINAL && !child.fragment.isInnerNode()) {
                    score = Float.NEGATIVE_INFINITY;
                }
                if(score > bestScore){
                    bestScore = score;
                }
            }
        }
        this.criticalPathScores[nodeIdx] = bestScore;
        return this.criticalPathScores[nodeIdx];
    }

    private void addCriticalPathToTree(CombinatorialNode node){
        CombinatorialNode currentNode = node;
        int crntNodeIdx = this.vertexIndices.get(currentNode);

        eachNode:
        while(this.criticalPathScores[crntNodeIdx] > 0){
            for(CombinatorialEdge edge : currentNode.outgoingEdges){
                CombinatorialNode child = edge.target;
                if(child.state<INSERTED) {  //!this.subtree.contains(child.fragment)){
                    int childIdx = this.vertexIndices.get(child);
                    float score = this.criticalPathScores[childIdx] + child.fragmentScore + edge.score;
                    if(this.criticalPathScores[crntNodeIdx] <= score){
                        this.subtree.addFragment(this.subtree.getNode(currentNode.fragment.bitset),child.fragment,edge.cut1, edge.cut2, child.fragmentScore,edge.score);

                        addFragment(child, edge);

                        currentNode = child;
                        crntNodeIdx = childIdx;
                        continue eachNode;
                    }
                }
            }
            throw new RuntimeException("Did not find correct path");
        }
    }

    private void addFragment(CombinatorialNode node, CombinatorialEdge graphEdge) {
        if (node.getFragment().isInnerNode()) {
            node.state = INSERTED;
        } else {
            node.state = INSERTED;
            graphEdge.getSource().state = ATTACHED_TO_TERMINAL;
        }
        //if (graphEdge!=null) System.out.println("Add " + node + " with " + graphEdge + " and score " + (node.score + graphEdge.score + criticalPathScores[vertexIndices.get(node)]));
        insertedNodes.add(node);
    }

    private CombinatorialNode addCriticalEdgeToTree(CombinatorialNode node){
        int nodeIdx = this.vertexIndices.get(node);
        float bestScore = Float.NEGATIVE_INFINITY;
        if(this.criticalPathScores[nodeIdx] > 0){
            for(CombinatorialEdge edge : node.outgoingEdges){
                CombinatorialNode child = edge.target;
                if(child.state < INSERTED) {//!this.subtree.contains(child.fragment)){
                    int childIdx = this.vertexIndices.get(child);
                    float score = this.criticalPathScores[childIdx] + child.fragmentScore + edge.score;
                    bestScore = Math.max(bestScore,score);
                    if(this.criticalPathScores[nodeIdx] == score){
                        this.subtree.addFragment(this.subtree.getNode(node.fragment.bitset), child.fragment, edge.cut1, edge.cut2, child.fragmentScore, edge.score);
                        addFragment(child, edge);
                        return child;
                    }
                }
            }
        }
        //System.out.println("Do not find path from " + node +  " with score " + this.criticalPathScores[nodeIdx] + ", best score is " + bestScore );
        return null;
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
        if(this.addCompletePath){
            return "CriticalPath1";
        }else{
            return "CriticalPath2";
        }
    }
}
