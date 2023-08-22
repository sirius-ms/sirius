package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IBond;

import java.io.*;
import java.util.*;

public class CombinatorialSubtree implements Iterable<CombinatorialNode> {

    private float score; // Summe der Kanten- und Knotenscores
    private final CombinatorialNode root;
    private final ArrayList<CombinatorialNode> nodes; // um Verwirrung zu vermeiden, sind auch hier alle Knoten ausser der Wurzel enthalten
    private final HashMap<BitSet, CombinatorialNode> bitset2Node;
    private final HashSet<CombinatorialFragment> fragments;

    public CombinatorialSubtree(MolecularGraph molecule){
        this.root = new CombinatorialNode(molecule.asFragment());
        this.root.depth = 0;
        this.root.bondbreaks = 0;
        this.score = 0;
        this.nodes = new ArrayList<>();
        this.bitset2Node = new HashMap<>();
        this.bitset2Node.put(this.root.fragment.bitset, this.root);

        fragments = new HashSet<>();
    }

    public HashMap<CombinatorialNode, Integer> getSubtreeSizes() {
        HashMap<CombinatorialNode, Integer> sizes = new HashMap<>(nodes.size());
        subtreeSizesRekursive(root, sizes);
        return sizes;
    }

    private void subtreeSizesRekursive(CombinatorialNode root, HashMap<CombinatorialNode, Integer> sizes) {
        int size=1;
        for (CombinatorialEdge e : root.getOutgoingEdges()) {
            subtreeSizesRekursive(e.target, sizes);
            size += sizes.get(e.target);
        }
        sizes.put(root, size);
    }

    public CombinatorialNode addFragment(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, float fragmentScore, float edgeScore){
        CombinatorialNode node = this.bitset2Node.get(fragment.bitset);
        if(node == null && this.bitset2Node.get(parent.fragment.bitset) == parent){
            node = new CombinatorialNode(fragment);
            node.depth = (short) (parent.depth + 1);
            node.bondbreaks = (short) (parent.bondbreaks + (firstBond != null ? 1 :0) + (secondBond != null ? 1 : 0));
            node.fragmentScore = fragmentScore;
            node.score = fragmentScore + edgeScore;
            node.totalScore = parent.totalScore + node.score;

            boolean cut1Direction = (firstBond != null) && fragment.bitset.get(firstBond.getAtom(0).getIndex());
            boolean cut2Direction = (secondBond != null) && fragment.bitset.get(secondBond.getAtom(0).getIndex());

            CombinatorialEdge edge = new CombinatorialEdge(parent, node, firstBond, secondBond, cut1Direction, cut2Direction);
            edge.score = edgeScore;
            node.incomingEdges.add(edge);
            parent.outgoingEdges.add(edge);

            this.score = this.score + node.fragmentScore + edge.score;
            this.nodes.add(node);
            this.bitset2Node.put(node.fragment.bitset, node);

            fragments.add(node.fragment);

            return node;
        }else {
            return null;
        }
    }

    public boolean removeSubtree(CombinatorialFragment fragment){
        CombinatorialNode node = this.bitset2Node.get(fragment.bitset);
        if(node != null && node != this.root){
            // because node != root, there is one incoming edge
            CombinatorialEdge edge = node.incomingEdges.get(0);
            CombinatorialNode parent = edge.source;
            parent.outgoingEdges.remove(edge);
            node.incomingEdges.remove(edge);

            double subtreeScore = node.fragmentScore;
            ArrayList<CombinatorialNode> subtreeNodes = new ArrayList<>();
            subtreeNodes.add(node);
            while(!subtreeNodes.isEmpty()){
                CombinatorialNode currentNode = subtreeNodes.remove(0);
                this.nodes.remove(currentNode);
                this.bitset2Node.remove(currentNode.fragment.bitset, currentNode);

                fragments.remove(currentNode.fragment);

                for(CombinatorialEdge e : currentNode.outgoingEdges){
                    CombinatorialNode child = e.target;
                    subtreeNodes.add(child);
                    subtreeScore = subtreeScore + e.score + child.fragmentScore;
                }
            }

            this.score = (float) (this.score - subtreeScore - edge.score);
            return true;
        }else{
            return false;
        }
    }

    public boolean replaceSubtree(CombinatorialFragment newParent, CombinatorialFragment newChild, IBond firstBond, IBond secondBond, float edgeScore){
        if(this.replaceSubtreeWithoutUpdate(newParent, newChild, firstBond, secondBond, edgeScore)){
            this.updateSubtree(this.bitset2Node.get(newChild.bitset));
            return true;
        }
        return false;
    }

    public boolean replaceSubtreeWithoutUpdate(CombinatorialFragment newParent, CombinatorialFragment newChild, IBond firstBond, IBond secondBond, float edgeScore){
        CombinatorialNode parentNode = this.bitset2Node.get(newParent.bitset);
        CombinatorialNode childNode = this.bitset2Node.get(newChild.bitset);
        if(parentNode != null && childNode != null && childNode != this.root){
            CombinatorialEdge edge = childNode.incomingEdges.get(0);
            childNode.incomingEdges.remove(edge);
            edge.source.outgoingEdges.remove(edge);

            boolean cut1Direction = (firstBond != null) && newChild.bitset.get(firstBond.getAtom(0).getIndex());
            boolean cut2Direction = (secondBond != null) && newChild.bitset.get(secondBond.getAtom(0).getIndex());

            CombinatorialEdge newEdge = new CombinatorialEdge(parentNode, childNode, firstBond, secondBond, cut1Direction, cut2Direction);
            newEdge.score = edgeScore;
            parentNode.outgoingEdges.add(newEdge);
            childNode.incomingEdges.add(newEdge);

            this.score = this.score - edge.score + newEdge.score;
            return true;
        }else{
            return false;
        }
    }

    public void update(){
        this.updateSubtree(this.root);
    }

    private void updateSubtree(CombinatorialNode subtreeRoot){
        if(subtreeRoot != this.root){
            CombinatorialEdge edge = subtreeRoot.incomingEdges.get(0);
            subtreeRoot.score = subtreeRoot.fragmentScore + edge.score;
            subtreeRoot.totalScore = edge.source.totalScore + subtreeRoot.score;
            subtreeRoot.depth = (short) (edge.source.depth + 1);
            subtreeRoot.bondbreaks = (short) (edge.source.bondbreaks + ((edge.cut1 != null) ? 1 : 0) + ((edge.cut2 != null) ? 1 : 0));
        }
        ArrayList<CombinatorialNode> queue = new ArrayList<>();
        for(CombinatorialEdge e : subtreeRoot.outgoingEdges) queue.add(e.target);

        while(!queue.isEmpty()){
            CombinatorialNode node = queue.remove(0);
            CombinatorialEdge edge = node.incomingEdges.get(0);

            node.score = edge.score + node.fragmentScore;
            node.totalScore = edge.source.totalScore + node.score;
            node.depth = (short) (edge.source.depth + 1);
            node.bondbreaks = (short) (edge.source.bondbreaks + (edge.cut1 != null ? 1 : 0) + (edge.cut2 != null ? 1 : 0));

            for(CombinatorialEdge e : node.outgoingEdges) queue.add(e.target);
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

    public ArrayList<CombinatorialEdge> getEdgeList(){
        // each node (except the root) has exactly one incoming edge:
        ArrayList<CombinatorialEdge> edgeList = new ArrayList<>(this.nodes.size());
        for(CombinatorialNode node : this.nodes) edgeList.add(node.incomingEdges.get(0));
        return edgeList;
    }

    public ArrayList<CombinatorialNode> getLeaves(){
        // every node (except the root) is considered a leaf if it doesn't have outgoing edges.
        ArrayList<CombinatorialNode> leaves = new ArrayList<>();
        for(CombinatorialNode node : this.nodes){
            if(node.getOutgoingEdges().isEmpty()) leaves.add(node);
        }
        return leaves;
    }

    public ArrayList<CombinatorialNode> getTerminalNodes(){
        // every node whose bitset.length is greater than the number of atoms (in the molecule) is a terminal node:
        ArrayList<CombinatorialNode> terminalNodes = new ArrayList<>();
        for(CombinatorialNode node : this.nodes){
            if(!node.fragment.isInnerNode()) terminalNodes.add(node);
        }
        return terminalNodes;
    }

    public ArrayList<CombinatorialNode> getAllParentsOf(CombinatorialNode node){
        ArrayList<CombinatorialNode> parents = new ArrayList<>(node.depth);
        CombinatorialNode currentNode = node;
        while(!currentNode.getIncomingEdges().isEmpty()){
            CombinatorialEdge inEdge = currentNode.getIncomingEdges().get(0);
            CombinatorialNode parent = inEdge.getSource();
            parents.add(parent);
            currentNode = parent;
        }
        return parents;
    }

    public double[][] getAdjacencyMatrix(){
        ArrayList<CombinatorialNode> sortedNodeList = this.getSortedNodeList();
        TObjectIntHashMap<CombinatorialNode> nodeIndices = new TObjectIntHashMap<>(this.numberOfNodes());
        for(int i = 0; i < this.numberOfNodes(); i++) nodeIndices.put(sortedNodeList.get(i), i);

        double[][] adjMatrix = new double[this.numberOfNodes()][this.numberOfNodes()];
        for(double[] row : adjMatrix) Arrays.fill(row, Double.NEGATIVE_INFINITY);

        for(int i = 0; i < this.numberOfNodes(); i++){
            CombinatorialNode node = sortedNodeList.get(i);
            for(CombinatorialEdge edge : node.outgoingEdges){
                CombinatorialNode adjNode = edge.target;
                int adjNodeIdx = nodeIndices.get(adjNode);
                adjMatrix[i][adjNodeIdx] = edge.score + adjNode.fragmentScore;
            }
        }

        return adjMatrix;
    }

    /**
     * Returns an array of length 2 containing the number of cuts regarding this given bond.<br>
     * The first position of the returned integer array is the number of cuts where
     * the first atom is contained in the fragment. The second position of the returned array is the number
     * of cuts where the second atom is contained in the fragment.
     *
     * @param bond {@link IBond} which is part of the molecule
     * @return an integer array of length 2 containing the amount of cuts regarding the given bond
     */
    public int[] getNumberOfCuts(IBond bond){
        int[] numberOfCuts = new int[2];

        // Every vertex (except the root) has an in-degree of 1 --> one incoming edge:
        for(CombinatorialNode node : this.nodes){
            CombinatorialEdge edge = node.incomingEdges.get(0);

            if(edge.getCut1() == bond){
                if(edge.getDirectionOfFirstCut()){
                    numberOfCuts[0]++;
                }else{
                    numberOfCuts[1]++;
                }
            }else if(edge.getCut2() == bond){
                if(edge.getDirectionOfSecondCut()){
                    numberOfCuts[0]++;
                }else{
                    numberOfCuts[1]++;
                }
            }
        }
        return numberOfCuts;
    }

    public float getScore(){
        return this.score;
    }

    public boolean contains(CombinatorialFragment fragment){
        return this.bitset2Node.get(fragment.bitset) != null;
    }

    public CombinatorialNode getNode(BitSet fragment){
        return this.bitset2Node.get(fragment);
    }

    public List<CombinatorialNode> getNodes(){
        return (ArrayList<CombinatorialNode>) this.nodes.clone();
    }

    public CombinatorialNode getRoot(){
        return this.root;
    }

    public int numberOfNodes(){
        return this.nodes.size() + 1;
    }

    @Override
    public Iterator<CombinatorialNode> iterator(){
        return new Iterator<CombinatorialNode>() {
            private int k = 0; // k=0 ist Wurzel, und k > 0 entspricht Index des Knotes in Liste nodes (von 1 aus gezählt)

            @Override
            public boolean hasNext() {
                return k <= nodes.size();
            }

            @Override
            public CombinatorialNode next() {
                if(!hasNext()) throw new NoSuchElementException();

                CombinatorialNode node;
                if(k == 0){
                    node = root;
                }else{
                    node = nodes.get(k-1);
                }
                k++;
                return node;
            }
        };
    }

    // todo
    private String nodeString(CombinatorialNode node){
        if(node == this.root){
            return node.fragment.toSMILES()+"[0,"+node.fragmentScore+",0];";
        }else {
            CombinatorialEdge edge = node.incomingEdges.get(0);
            if(node.fragment.innerNode) {
                return node.fragment.toSMILES() + "[" + edge.score + "," + node.fragmentScore + "," + node.bondbreaks + "]";
            }else{
                return node.fragment.getFormula() + "[" + edge.score + "," + node.fragmentScore + "," + node.bondbreaks + "]";
            }
        }
    }

    public String toNewickString(CombinatorialNode currentNode){
        StringBuilder strBuilder = new StringBuilder();

        for(int i = 0; i < currentNode.outgoingEdges.size(); i++){
            if(i == 0){
                strBuilder.append("(");
            }

            CombinatorialNode node = currentNode.outgoingEdges.get(i).target;
            strBuilder.append(this.toNewickString(node));

            if(i < currentNode.outgoingEdges.size()-1){
                strBuilder.append(",");
            }else{
                strBuilder.append(")");
            }
        }

        strBuilder.append(this.nodeString(currentNode));
        return strBuilder.toString();
    }

    public String toJsonString(){
        try {
            return CombinatorialSubtreeJsonWriter.treeToJsonString(this);
        } catch (CDKException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString(){
        return this.toNewickString(this.root);
    }

    /**
     * Converts this {@link CombinatorialSubtree} into a boolean array denoting
     * which edges of {@code graph} are contained in this subtree.<br>
     *
     * @param graph the {@link CombinatorialGraph} that respresents the supergraph of this subtree
     * @return an boolean array denoting which edges of {@code graph} are present in this subtree
     */
    public boolean[] toBooleanArray(CombinatorialGraph graph){
        int maxBitSetLength = graph.maximalBitSetLength();
        TObjectIntHashMap<BitSet> mergedEdgeBitSet2Index = graph.mergedEdgeBitSet2Index();
        return this.toBooleanArray(mergedEdgeBitSet2Index, maxBitSetLength);
    }

    /**
     * Converts this {@link CombinatorialSubtree} into a boolean array denoting
     * which edges of {@code graph} are contained in this subtree.<br>
     *
     * The given hashmap {@code mergedEdgeBitSet2Index} assigns each {@link BitSet}, that is the result
     * of merging the source and target BitSet of an {@link CombinatorialEdge} contained in the
     * underlying {@link CombinatorialGraph}, a unique integer value
     * in the range from 0 to #edgesInCombGraph. Because every {@link CombinatorialNode} in {@link CombinatorialGraph}
     * has a unique BitSet object, every merged edge BitSet is also unique.<br>
     *
     * @param mergedEdgeBitSet2Index hashmap that assigns each edge a unique index in the resulting array
     * @param maxBitSetLength the maximal length of all bitsets found in the {@link CombinatorialGraph}
     * @return a boolean array denoting which edges of the supergraph are contained in this subtree
     */
    public boolean[] toBooleanArray(TObjectIntHashMap<BitSet> mergedEdgeBitSet2Index, int maxBitSetLength){
        boolean[] subtreeArray = new boolean[mergedEdgeBitSet2Index.size()];

        for(CombinatorialNode node : this.nodes){
            CombinatorialEdge edge = node.incomingEdges.get(0);
            BitSet mergedEdgeBitSet = edge.getMergedBitSet(maxBitSetLength);
            int edgeIdx = mergedEdgeBitSet2Index.get(mergedEdgeBitSet);
            subtreeArray[edgeIdx] = true;
        }

        return subtreeArray;
    }
}
