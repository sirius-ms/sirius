package de.unijena.bioinf.fragmenter;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;

public class CombinatorialSubtreeManipulator {

    public static double removeDanglingSubtrees(CombinatorialSubtree tree){
        // 1. Initialisation
        TObjectIntHashMap<CombinatorialNode> indices = new TObjectIntHashMap<>();
        int idx = 0;
        for(CombinatorialNode node : tree){
            indices.put(node,idx);
            idx++;
        }
        // 2. On-the-fly removal of subtrees with negative profit:
        return getBestSubtreeScore(tree.getRoot(), tree, indices);
    }

    private static double getBestSubtreeScore(CombinatorialNode currentNode, CombinatorialSubtree tree, TObjectIntHashMap<CombinatorialNode> indices){
        double score = 0;
        ArrayList<CombinatorialEdge> outgoingEdges = new ArrayList<>(currentNode.getOutgoingEdges());

        for(CombinatorialEdge edge : outgoingEdges){
            CombinatorialNode child = edge.target;
            double childScore = getBestSubtreeScore(child, tree, indices) + child.fragmentScore + edge.score;

            if(childScore < 0){ // retaining the subtree below 'child' results in a smaller profit of this whole tree
                tree.removeSubtree(child.fragment);
            }else{
                score = score + childScore;
            }
        }
        return score;
    }
}
