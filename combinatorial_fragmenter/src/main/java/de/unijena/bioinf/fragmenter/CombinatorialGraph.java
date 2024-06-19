package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.util.*;

public class CombinatorialGraph {

    protected final ArrayList<CombinatorialNode> nodes; //alle Knoten außer der Wurzel
    protected final CombinatorialNode root;
    protected final HashMap<BitSet, CombinatorialNode> bitset2node;

    public CombinatorialGraph(MolecularGraph graph) {
        this.root = new CombinatorialNode(graph.asFragment());
        root.score =0;
        root.depth = 0;
        root.bondbreaks = 0;
        this.nodes = new ArrayList<>();
        this.bitset2node = new HashMap<>();
        bitset2node.put(root.fragment.bitset, root);
    }



    public CombinatorialNode addReturnNovel(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, CombinatorialFragmenterScoring scoring) {
        boolean novel = (bitset2node.get(fragment.bitset) == null);
        CombinatorialNode node = this.addReturnAlways(parent, fragment,firstBond,secondBond,scoring,null);
        if(novel){
            return node;
        }else{
            return null;
        }
    }

    public CombinatorialNode addReturnAlways(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, CombinatorialFragmenterScoring scoring, boolean[] updateFlag) {
        CombinatorialNode node = bitset2node.get(fragment.bitset);
        if (node == null) {
            node = new CombinatorialNode(fragment);
            bitset2node.put(fragment.bitset,node);
            nodes.add(node);
            node.score = Float.NEGATIVE_INFINITY;
            node.totalScore = Float.NEGATIVE_INFINITY;
        }

        node.depth = (short)Math.min(node.depth, parent.depth+1);
        node.bondbreaks = (short)Math.min(node.bondbreaks, parent.bondbreaks + (firstBond != null ? 1 : 0) + (secondBond !=null ? 1 : 0));

        boolean cut1Direction = firstBond != null && (fragment.bitset.get(firstBond.getAtom(0).getIndex()));
        boolean cut2Direction = secondBond != null && (fragment.bitset.get(secondBond.getAtom(0).getIndex()));

        CombinatorialEdge edge = new  CombinatorialEdge(parent, node, firstBond, secondBond,cut1Direction,cut2Direction);
        node.incomingEdges.add(edge);
        parent.outgoingEdges.add(edge);

        edge.score = (float) scoring.scoreEdge(edge);
        node.fragmentScore = (float) scoring.scoreFragment(node);
        float score = node.fragmentScore + edge.score;
        float bestScore = (parent.totalScore + score);

        if (bestScore > node.totalScore) {
            node.totalScore = bestScore;
            node.score = score;
            if(updateFlag != null && updateFlag.length > 0) updateFlag[0] = true;
        } else {
            if(updateFlag != null && updateFlag.length > 0) updateFlag[0] = false;
        }
        return node;
    }

    public CombinatorialEdge deleteEdge(CombinatorialNode source, CombinatorialNode target){
        CombinatorialEdge edge = null;
        for(CombinatorialEdge e : source.outgoingEdges){
            if(e.target == target){
                edge = e;
                break;
            }
        }
        if(edge == null || target.incomingEdges.size() == 1) return null;

        source.outgoingEdges.remove(edge);
        target.incomingEdges.remove(edge);

        // the scores of 'source' haven't changed, but maybe for 'target'
        target.bondbreaks = Short.MAX_VALUE;
        target.depth = Short.MAX_VALUE;
        target.totalScore = Float.NEGATIVE_INFINITY;

        for(CombinatorialEdge e : target.incomingEdges){
            target.bondbreaks = (short) Math.min(target.bondbreaks, e.source.bondbreaks + (e.cut1 != null ? 1 : 0) + (e.cut2 != null ? 1 : 0));
            target.depth = (short) Math.min(target.depth, e.source.depth +1);

            float score = target.fragmentScore + e.score;
            float bestScore = e.source.totalScore + score;
            if(bestScore > target.totalScore){
                target.totalScore = bestScore;
                target.score = score;
            }
        }

        return edge;
    }

    /**
     * This method removes the {@link CombinatorialNode} {@code node} from this CombinatorialGraph
     * in a "dangerous" fashion because it can happen that the resulting graph is not connected and
     * the attributes of the successor nodes are not valid anymore.
     * Thus, use this method carefully.<br>
     * All nodes in this graph except the root can be removed.
     *
     * @param node the {@link CombinatorialNode} to remove
     * @return {@code true} if this node was removed successfully from this graph;<br>
     * {@code false} if this node is not contained in this graph, is the root of this graph or is assigned with {@code null}
     */
    public boolean deleteNodeDangerously(CombinatorialNode node){
        if(node != null && node != this.root && node == this.bitset2node.get(node.fragment.bitset)){
            // 1: Delete this node from this.nodes and this.bitset2node:
            this.nodes.remove(node);
            this.bitset2node.remove(node.fragment.bitset, node);

            // 2: Delete all pointers to this node from its parent and child nodes:
            for(CombinatorialEdge inEdge : node.incomingEdges){
                CombinatorialNode parentNode = inEdge.source;
                parentNode.outgoingEdges.remove(inEdge);
            }

            for(CombinatorialEdge outEdge : node.outgoingEdges){
                CombinatorialNode childNode = outEdge.target;
                childNode.incomingEdges.remove(outEdge);
            }

            // 3.: Delete all pointer to its parent and child nodes:
            node.incomingEdges.clear();
            node.outgoingEdges.clear();

            return true;
        }else{
            return false;
        }
    }

    /**
     * for each node, allow only paths back to root which have minimal distance
     */
    public void pruneLongerPaths() {
        for (CombinatorialNode node : nodes) {
            final int min = node.incomingEdges.stream().mapToInt(x->x.source.bondbreaks).min().orElse(0);
            node.incomingEdges.removeIf(combinatorialEdge -> combinatorialEdge.source.bondbreaks > min);
        }
    }

    /**
     * Returns an {@link ArrayList} containing all nodes in this graph which are sorted
     * regarding to their {@link BitSet} object. In this case, the {@link BitSet} represents a binary number.
     *
     * @return a list contained all nodes sorted regarding to their {@link BitSet}
     */
    public ArrayList<CombinatorialNode> getSortedNodeList(){
        ArrayList<CombinatorialNode> sortedList = new ArrayList<>(this.nodes);
        sortedList.add(this.root);
        sortedList.sort((n1, n2) -> {
            int num1 = 0, num2 = 0;
            for (int i = 0; i <= this.root.fragment.parent.natoms; i++) {
                num1 = num1 + (n1.fragment.bitset.get(i) ? (int) Math.pow(2, i) : 0);
                num2 = num2 + (n2.fragment.bitset.get(i) ? (int) Math.pow(2, i) : 0);
            }
            return num1 - num2;
        });
        return sortedList;
    }

    /**
     * Returns the adjacency matrix of this {@link CombinatorialGraph} object.<br>
     * Each row and column represents a pair of {@link CombinatorialNode} objects contained in this graph.
     * The nodes are ordered in respect to their {@link BitSet}.
     *
     * @return adjacency matrix of this graph
     */
    public double[][] getAdjacencyMatrix(){
        ArrayList<CombinatorialNode> sortedNodeList = this.getSortedNodeList();
        TObjectIntHashMap<CombinatorialNode> nodeIndices = new TObjectIntHashMap<>(this.numberOfNodes());
        for(int i = 0; i < this.numberOfNodes(); i++) nodeIndices.put(sortedNodeList.get(i),i);

        double[][] adjMatrix = new double[this.numberOfNodes()][this.numberOfNodes()];
        for(double[] row : adjMatrix) Arrays.fill(row, Double.NEGATIVE_INFINITY);

        for(int i = 0; i < adjMatrix.length; i++){
            CombinatorialNode node = sortedNodeList.get(i);
            for(CombinatorialEdge edge : node.outgoingEdges){
                int adjNodeIdx = nodeIndices.get(edge.target);
                adjMatrix[i][adjNodeIdx] = edge.score + edge.target.fragmentScore;
            }
        }
        return adjMatrix;
    }

    public ArrayList<CombinatorialEdge> getEdgeList(){
        // each node can have more than one incoming edge:
        ArrayList<CombinatorialEdge> edgeList = new ArrayList<>();
        for(CombinatorialNode node : this.nodes){
            edgeList.addAll(node.incomingEdges);
        }
        return edgeList;
    }

    /**
     * This method returns a hashmap which assigns each CombinatorialEdge in this graph a unique index with
     * index &ge; 0 and index &le; #Edges.<br>
     * This hashmap can be used to transform a {@link CombinatorialSubtree} into a binary array denoting
     * which edges are contained and which are not contained in the subtree.
     *
     */
    public TObjectIntHashMap<CombinatorialEdge> getEdgeIndices(){
        TObjectIntHashMap<CombinatorialEdge> edgeIndices = new TObjectIntHashMap<>();
        int idx = 0;
        for(CombinatorialNode node : this.nodes){
            for(CombinatorialEdge edge : node.incomingEdges){
                edgeIndices.put(edge, idx);
                idx++;
            }
        }
        return edgeIndices;
    }

    public TObjectIntHashMap<BitSet> mergedEdgeBitSet2Index(){
        int maxBitSetLength = this.maximalBitSetLength();
        TObjectIntHashMap<BitSet> mergedEdgeBitSet2Index = new TObjectIntHashMap<>();

        int idx = 0;
        for(CombinatorialNode node : this.nodes){
            for(CombinatorialEdge edge : node.incomingEdges){
                BitSet mergedEdgeBitSet = edge.getMergedBitSet(maxBitSetLength);
                mergedEdgeBitSet2Index.put(mergedEdgeBitSet, idx);
                idx++;
            }
        }

        return mergedEdgeBitSet2Index;
    }

    public int maximalBitSetLength(){
        int maxBitSetLength = this.root.fragment.bitset.length();
        for(CombinatorialNode node : this.nodes){
            int nodeBitSetLength = node.fragment.bitset.length();
            if(nodeBitSetLength > maxBitSetLength){
                maxBitSetLength = nodeBitSetLength;
            }
        }
        return maxBitSetLength;
    }

    public boolean contains(CombinatorialFragment fragment){
        return this.bitset2node.get(fragment.bitset) != null;
    }

    public CombinatorialNode getNode(BitSet fragment){
        return this.bitset2node.get(fragment);
    }

    public List<CombinatorialNode> getNodes() {
        return nodes;
    }

    public List<CombinatorialNode> getTerminalNodes(){
        ArrayList<CombinatorialNode> terminalNodeList = new ArrayList<>();
        for(CombinatorialNode node : this.nodes){
            if(!node.fragment.isInnerNode()){
                terminalNodeList.add(node);
            }
        }
        return terminalNodeList;
    }

    public CombinatorialNode getRoot() {
        return root;
    }

    public int numberOfNodes(){
        return this.nodes.size() + 1;
    }
}
