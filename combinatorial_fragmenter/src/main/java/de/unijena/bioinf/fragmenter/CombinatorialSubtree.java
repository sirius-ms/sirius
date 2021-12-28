package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IBond;

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

            float subtreeScore = node.fragmentScore;
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

            this.score = this.score - subtreeScore - edge.score;
            return true;
        }else{
            return false;
        }
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
            if(node.fragment.isRealFragment) {
                CombinatorialEdge edge = node.incomingEdges.get(0);
                return node.fragment.toSMILES() + "[" + edge.score + "," + node.fragmentScore + "," + node.bondbreaks + "]";
            }else{
                // in this case, node is an artificially added CombinatorialNode --> terminal node
                // all nodes that are connected to this terminal node share the same molecular formula
                CombinatorialEdge edge = node.incomingEdges.get(0);
                CombinatorialNode parentNode = edge.source;
                return parentNode.fragment.getFormula().toString() + "[" + edge.score + "," + node.fragmentScore + "," + node.bondbreaks + "]";
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

    @Override
    public String toString(){
        return this.toNewickString(this.root);
    }
}
