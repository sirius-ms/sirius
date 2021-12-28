package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;
import java.util.List;

public class CriticalPathSubtreeCalculator extends CombinatorialSubtreeCalculator{

    private boolean isInitialized, isComputed;
    private CombinatorialGraph graph;
    private TObjectIntHashMap<CombinatorialNode> vertexIndices;
    private double[] criticalPathScores;

    private final boolean addCompletePath;

    public CriticalPathSubtreeCalculator(FTree fTree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring, boolean addCompletePath){
        super(fTree, molecule, scoring);
        this.isInitialized = false;
        this.isComputed = false;
        this.addCompletePath = addCompletePath;
    }

    public void initialize(CombinatorialFragmenter.Callback2 furtherFragmentation){
        if(this.isInitialized) throw new IllegalStateException("This object is already initialised.");

        // 1. Create a CombinatorialFragmentationGraph:
        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(molecule,scoring);
        this.graph = fragmenter.createCombinatorialFragmentationGraph(furtherFragmentation);
        CombinatorialGraphManipulator.addTerminalNodes(this.graph, this.scoring, this.fTree);

        // 2. Determine a hashmap which assigns each CombinatorialNode in this.graph a specific integer index.
        this.vertexIndices = new TObjectIntHashMap<>();
        this.vertexIndices.put(this.graph.getRoot(),0);

        List<CombinatorialNode> nodes = this.graph.getNodes(); // all nodes in this.graph except the root
        for(int idx = 0; idx < nodes.size(); idx++){
            this.vertexIndices.put(nodes.get(idx), idx+1);
        }

        // 3. Initialise the array this.criticalPathScores:
        this.criticalPathScores = new double[nodes.size() +1];
        Arrays.fill(this.criticalPathScores, Double.NaN);

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
            Arrays.fill(this.criticalPathScores, Double.NaN);

            maxScoreNode = this.graph.root;
            double bestScore = this.calculateCriticalPathScore(maxScoreNode);
            for(CombinatorialNode node : this.graph.getNodes()){
                if(this.subtree.contains(node.fragment)){
                    double score = this.calculateCriticalPathScore(node);
                    if(score > bestScore){
                        maxScoreNode = node;
                        bestScore = score;
                    }
                }
            }
            maxScoreNodeIdx = this.vertexIndices.get(maxScoreNode);
        }

        this.isComputed = true;
        this.score = this.subtree.getScore();
        return this.subtree;
    }

    private double calculateCriticalPathScore(CombinatorialNode node) {
        int nodeIdx = this.vertexIndices.get(node);
        // check, if the CriticalPath score of node was calculated
        if(!Double.isNaN(this.criticalPathScores[nodeIdx])) return this.criticalPathScores[nodeIdx];

        double bestScore = Double.NEGATIVE_INFINITY;
        for(CombinatorialEdge edge : node.outgoingEdges){
            CombinatorialNode child = edge.target;
            if(!this.subtree.contains(child.fragment)){
                double score = this.calculateCriticalPathScore(child) + child.fragmentScore + edge.score;
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

        while(this.criticalPathScores[crntNodeIdx] > 0){
            for(CombinatorialEdge edge : currentNode.outgoingEdges){
                CombinatorialNode child = edge.target;
                if(!this.subtree.contains(child.fragment)){
                    int childIdx = this.vertexIndices.get(child);
                    double score = this.criticalPathScores[childIdx] + edge.score + child.fragmentScore;
                    if(this.criticalPathScores[crntNodeIdx] == score){
                        this.subtree.addFragment(this.subtree.getNode(currentNode.fragment.bitset),child.fragment,edge.cut1, edge.cut2, child.fragmentScore,edge.score);
                        currentNode = child;
                        crntNodeIdx = childIdx;
                        break;
                    }
                }
            }
        }
    }

    private void addCriticalEdgeToTree(CombinatorialNode node){
        int nodeIdx = this.vertexIndices.get(node);
        if(this.criticalPathScores[nodeIdx] > 0){
            for(CombinatorialEdge edge : node.outgoingEdges){
                CombinatorialNode child = edge.target;
                if(!this.subtree.contains(child.fragment)){
                    int childIdx = this.vertexIndices.get(child);
                    double score = this.criticalPathScores[childIdx] + child.fragmentScore + edge.score;
                    if(this.criticalPathScores[nodeIdx] == score){
                        this.subtree.addFragment(this.subtree.getNode(node.fragment.bitset), child.fragment, edge.cut1, edge.cut2, child.fragmentScore, edge.score);
                        break;
                    }
                }
            }
        }
    }

    public boolean isInitialized(){
        return this.isInitialized;
    }

    public boolean isComputed(){
        return this.isComputed;
    }
}
