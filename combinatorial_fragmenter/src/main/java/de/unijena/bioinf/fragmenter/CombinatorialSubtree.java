package de.unijena.bioinf.fragmenter;

import gnu.trove.map.hash.TObjectIntHashMap;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IBond;

import java.io.IOException;
import java.util.*;

public class CombinatorialSubtree implements Iterable<CombinatorialNode> {

    private float score; // Summe der Kanten- und Knotenscores
    private final CombinatorialNode root;
    private final ArrayList<CombinatorialNode> nodes; // um Verwirrung zu vermeiden, sind auch hier alle Knoten ausser der Wurzel enthalten
    private final HashMap<BitSet, CombinatorialNode> bitset2Node;

    public CombinatorialSubtree(MolecularGraph molecule){
        this.root = new CombinatorialNode(molecule.asFragment());
        this.root.depth = 0;
        this.root.bondbreaks = 0;
        this.score = 0;
        this.nodes = new ArrayList<>();
        this.bitset2Node = new HashMap<>();
        this.bitset2Node.put(this.root.fragment.bitset, this.root);
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
            if(node.fragment.isRealFragment) {
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

    public int[] toBinaryArray(CombinatorialGraph graph){
        throw new UnsupportedOperationException();
    }
}
