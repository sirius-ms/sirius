package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IBond;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class CombinatorialSubtree {

    private final CombinatorialNode root;
    private final ArrayList<CombinatorialNode> nodes; // um Verwirrung zu vermeiden, sind auch hier alle Knoten ausser der Wurzel enthalten
    private final HashMap<BitSet, CombinatorialNode> bitset2Node;

    public CombinatorialSubtree(MolecularGraph molecule){
        this.root = new CombinatorialNode(molecule.asFragment());
        this.root.depth = 0;
        this.root.bondbreaks = 0;
        this.nodes = new ArrayList<>();
        this.bitset2Node = new HashMap<>();
        this.bitset2Node.put(this.root.fragment.bitset, this.root);
    }

    public CombinatorialNode addFragment(CombinatorialNode parent, CombinatorialFragment fragment, IBond firstBond, IBond secondBond, float fragmentScore, float edgeScore){
        CombinatorialNode node = this.bitset2Node.get(fragment.bitset);
        if(node == null && this.bitset2Node.get(parent.fragment.bitset) == parent){
            node = new CombinatorialNode(fragment);
            node.depth = (short) (parent.depth + 1);
            node.bondbreaks = (short) (parent.bondbreaks + ((secondBond == null) ? 1 : 2) );
            node.fragmentScore = fragmentScore;
            node.score = fragmentScore + edgeScore;
            node.totalScore = parent.totalScore + node.score;

            boolean cut1Direction = fragment.bitset.get(firstBond.getAtom(0).getIndex());
            boolean cut2Direction = (secondBond != null) && fragment.bitset.get(secondBond.getAtom(0).getIndex());

            CombinatorialEdge edge = new CombinatorialEdge(parent, node, firstBond, secondBond, cut1Direction, cut2Direction);
            edge.score = edgeScore;
            node.incomingEdges.add(edge);
            parent.outgoingEdges.add(edge);

            this.nodes.add(node);
            this.bitset2Node.put(node.fragment.bitset, node);

            return node;
        }else {
            return null;
        }
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
    public String toString(){
        // todo
        return null;
    }
}
