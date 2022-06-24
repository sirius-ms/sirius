package de.unijena.bioinf.fragmenter;

import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;

public class CombinatorialSubtreeManipulator {

    public static double removeDanglingSubtrees(CombinatorialSubtree tree){
        // On-the-fly removal of subtrees with negative profit:
        return getBestSubtreeScore(tree.getRoot(), tree);
    }

    private static double getBestSubtreeScore(CombinatorialNode currentNode, CombinatorialSubtree tree){
        double score = 0;
        ArrayList<CombinatorialEdge> outgoingEdges = new ArrayList<>(currentNode.getOutgoingEdges());

        for(CombinatorialEdge edge : outgoingEdges){
            CombinatorialNode child = edge.target;
            double childScore = getBestSubtreeScore(child, tree) + child.fragmentScore + edge.score;

            if(childScore < 0){ // retaining the subtree below 'child' results in a smaller profit of this whole tree
                tree.removeSubtree(child.fragment);
            }else{
                score = score + childScore;
            }
        }
        return score;
    }

    public static double tanimoto(CombinatorialSubtree subtree1, CombinatorialSubtree subtree2, CombinatorialGraph graph){
        int maxBitSetLength = graph.maximalBitSetLength();
        TIntIntHashMap edgeValue2edgeIndex = graph.edgeValue2Index();
        return tanimoto(subtree1, subtree2, edgeValue2edgeIndex, maxBitSetLength);
    }

    public static double tanimoto(CombinatorialSubtree subtree1, CombinatorialSubtree subtree2, TIntIntHashMap edgeValue2edgeIdx, int maxBitSetLength){
        boolean[] subtree1Array = subtree1.toBooleanArray(edgeValue2edgeIdx, maxBitSetLength);
        boolean[] subtree2Array = subtree2.toBooleanArray(edgeValue2edgeIdx, maxBitSetLength);
        return tanimoto(subtree1Array, subtree2Array);
    }

    public static double tanimoto(boolean[] subtree1, boolean[] subtree2){
        int numCommonEdges = 0;
        int numEdges = 0;

        for(int i = 0; i < subtree1.length; i++){
            numCommonEdges = numCommonEdges + (subtree1[i] && subtree2[i] ? 1 : 0);
            numEdges = numEdges + (subtree1[i] || subtree2[i] ? 1 : 0);
        }

        return ((double) numCommonEdges / numEdges);
    }
}
