package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.*;

public class CriticalPathSubtreeCalculatorWithInsertion extends CombinatorialSubtreeCalculator{

    private boolean isInitialized, isComputed;
    private TObjectIntHashMap<CombinatorialNode> vertexIndices;
    private float[] criticalPathScores;
    private ArrayList<CombinatorialNode> insertedNodes;

    private final boolean addCompletePath;
    private HashMap<CombinatorialNode, ReInsertion> reinsertions = new HashMap<>();

    public CriticalPathSubtreeCalculatorWithInsertion(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring, boolean addCompletePath){
        super(fTree, molecule, scoring);
        this.isInitialized = false;
        this.isComputed = false;
        this.addCompletePath = addCompletePath;
        this.insertedNodes = new ArrayList<>();
    }

    private static class ReInsertion {
        private CombinatorialNode newNode, newChild, treeNode, treeParent;
        private CombinatorialEdge newEdge;
        private float score;
    }

    private ReInsertion checkForReinsertion(CombinatorialNode newNode) {
        double bestScore = 0d;
        ReInsertion best = new ReInsertion();
        for (CombinatorialEdge e : newNode.getOutgoingEdges()) {
            if (e.target.state==5) {
                final CombinatorialNode newChild = e.target;
                final CombinatorialNode treeNode = subtree.getNode(newChild.fragment.bitset);
                final CombinatorialNode treeParent = treeNode.incomingEdges.get(0).source;
                final float score = e.score - treeNode.incomingEdges.get(0).score;
                if (score > bestScore) {
                    best.newEdge = e;
                    best.newChild = newChild;
                    best.treeNode = treeNode;
                    best.treeParent = treeParent;
                    best.score = score;
                    bestScore = score;
                }
            }
        }
        return bestScore > 0 ? best : null;
    }

    public CriticalPathSubtreeCalculatorWithInsertion(FTree fTree, CombinatorialGraph graph, CombinatorialFragmenterScoring scoring, boolean addCompletePath){
        super(fTree, graph, scoring);
        this.isInitialized = false;
        this.isComputed = false;
        this.addCompletePath = addCompletePath;
        this.insertedNodes = new ArrayList<>();
    }

    public void initialize(CombinatorialFragmenter.Callback2 fragmentationConstraint){
        if(this.isInitialized) throw new IllegalStateException("This object is already initialised.");

        // 1. Create a CombinatorialFragmentationGraph - if it hasn't been computed yet:
        if(this.graph == null) {
            CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule, scoring);
            this.graph = fragmenter.createCombinatorialFragmentationGraph(fragmentationConstraint);
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

        this.isInitialized = true;
    }

    @Override
    public CombinatorialSubtree computeSubtree(){
        if(this.isComputed) return this.subtree;
        /* At this moment:
         * - the initial subtree is already initialised --> it contains only the root
         * - the array of critical path scores is initialised with NaN values
         *  --> these scores have to be computed now by calling calculateCriticalPathScore(graph.root)*/
        this.calculateCriticalPathScore(this.graph.root,false);
        this.graph.root.state=5;
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
            }else{
                this.addCriticalEdgeToTree(maxScoreNode);
            }
            Arrays.fill(this.criticalPathScores, Float.NaN);
            reinsertions.clear();

            maxScoreNode = this.graph.root;
            double bestScore = this.calculateCriticalPathScore(maxScoreNode,false);
            for(CombinatorialNode node : this.graph.getNodes()){
                if(node.state==5){//this.subtree.contains(node.fragment)){
                    double score = this.calculateCriticalPathScore(node,true);

                    if(score > bestScore){
                        maxScoreNode = node;
                        bestScore = score;
                    }
                }
            }
            maxScoreNodeIdx = this.vertexIndices.get(maxScoreNode);
        }
        removeDanglingSubtrees();

        this.isComputed = true;
        this.score = this.subtree.getScore();
        return this.subtree;
    }

    private void removeDanglingSubtrees() {
        boolean changed;
        do {
            Iterator<CombinatorialNode> iter = insertedNodes.listIterator();
            changed=false;
            while (iter.hasNext()) {
                final CombinatorialNode node = iter.next();
                final CombinatorialNode treeNode = subtree.getNode(node.fragment.bitset);
                if (treeNode==null) {
                    iter.remove();
                    changed=true;
                }
                else if (treeNode.getOutgoingEdges().isEmpty() && treeNode.score<0) {
                    subtree.removeSubtree(treeNode.fragment);
                    iter.remove();
                    changed=true;
                }
            }
        } while(changed);
    }

    private float calculateCriticalPathScore(CombinatorialNode node, boolean insertion) {
        int nodeIdx = this.vertexIndices.get(node);
        // check, if the CriticalPath score of node was calculated
        if(!Float.isNaN(this.criticalPathScores[nodeIdx])) {
            if (insertion) {
                float score = this.criticalPathScores[nodeIdx];
                if (insertion) {
                    ReInsertion reinsert = reinsertions.computeIfAbsent(node, this::checkForReinsertion);
                    if (reinsert!=null) {
                        score += reinsert.score;
                    }
                }
                return score;
            }
        }
        float bestScore = Float.NEGATIVE_INFINITY;
        for(CombinatorialEdge edge : node.outgoingEdges){
            CombinatorialNode child = edge.target;
            if(child.state<5) {//!this.subtree.contains(child.fragment)){
                float score = this.calculateCriticalPathScore(child,false) + child.fragmentScore + edge.score;
                if (insertion) {
                    ReInsertion reinsert = reinsertions.computeIfAbsent(child, this::checkForReinsertion);
                    if (reinsert!=null) {
                        score += reinsert.score;
                    }
                }
                if(score > bestScore){
                    bestScore = score;
                }
            }
        }
        this.criticalPathScores[nodeIdx] = Math.max(0,bestScore);
        return this.criticalPathScores[nodeIdx];
    }

    private void addCriticalPathToTree(CombinatorialNode node){
        CombinatorialNode currentNode = node;
        int crntNodeIdx = this.vertexIndices.get(currentNode);

        eachNode:
        while(this.criticalPathScores[crntNodeIdx] > 0){
            for(CombinatorialEdge edge : currentNode.outgoingEdges){
                CombinatorialNode child = edge.target;
                if(child.state<5) {  //!this.subtree.contains(child.fragment)){
                    int childIdx = this.vertexIndices.get(child);
                    double score = this.criticalPathScores[childIdx] + edge.score + child.fragmentScore;
                    if(this.criticalPathScores[crntNodeIdx] == score){
                        this.subtree.addFragment(this.subtree.getNode(currentNode.fragment.bitset),child.fragment,edge.cut1, edge.cut2, child.fragmentScore,edge.score);

                        addTotree(child);

                        currentNode = child;
                        crntNodeIdx = childIdx;
                        continue eachNode;
                    }
                }
            }
            throw new RuntimeException("Did not find correct path");
        }
    }

    private void addTotree(CombinatorialNode child) {
        child.state = 5;
        insertedNodes.add(child);
    }

    private void addCriticalEdgeToTree(CombinatorialNode node){
        int nodeIdx = this.vertexIndices.get(node);
        if(this.criticalPathScores[nodeIdx] > 0){
            for(CombinatorialEdge edge : node.outgoingEdges){
                CombinatorialNode child = edge.target;
                if(child.state < 5) {//!this.subtree.contains(child.fragment)){
                    int childIdx = this.vertexIndices.get(child);
                    double score = this.criticalPathScores[childIdx] + child.fragmentScore + edge.score;
                    ReInsertion re = reinsertions.get(child);
                    if (re!=null) score += re.score;
                    if(this.criticalPathScores[nodeIdx] == score){
                        this.subtree.addFragment(this.subtree.getNode(node.fragment.bitset), child.fragment, edge.cut1, edge.cut2, child.fragmentScore, edge.score);
                        addTotree(child);
                        if (re!=null) reinsert(re);
                        //System.out.println("Add edge " + edge.toString() +  " with score " + score + "    (" + edge.score + ")");
                        break;
                    }
                }
            }
        }
    }

    private void reinsert(ReInsertion re) {
        subtree.replaceSubtree(re.newNode.fragment, re.newChild.fragment, re.newEdge.cut1, re.newEdge.cut2, re.newEdge.score );
        re.newNode.state=5;
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
