package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import org.openscience.cdk.interfaces.IBond;

import java.util.*;

public class RuleBasedFragmentation extends AbstractFragmentationPredictor{

    private final CombinatorialFragmenterScoring scoring;
    private final CombinatorialFragmenter.Callback2 fragmentationConstraint;
    private final BitSet bondsToCut;
    private final int numFragments;

    public RuleBasedFragmentation(MolecularGraph molecule, CombinatorialFragmenterScoring scoring, int numFragments, FragmentationRules fragmentationRules, CombinatorialFragmenter.Callback2 fragmentationConstraint){
        super(molecule);
        this.numFragments = numFragments;
        this.scoring = scoring;
        this.fragmentationConstraint = fragmentationConstraint;

        this.bondsToCut = new BitSet(molecule.getBonds().length);
        for(IBond bond : molecule.getBonds()){
            int bondIdx = bond.getIndex();
            this.bondsToCut.set(bondIdx, fragmentationRules.match(bond));
        }
    }

    public RuleBasedFragmentation(MolecularGraph molecule, int numFragments, FragmentationRules fragmentationRules, CombinatorialFragmenter.Callback2 fragmentationConstraint){
        this(molecule, null, numFragments, fragmentationRules, fragmentationConstraint);
    }

    public CombinatorialGraph predictFragmentation() {
        // Construct the combinatorial fragmentation graph for this.molecule:
        CombinatorialFragmenter fragmenter = this.scoring != null ? new CombinatorialFragmenter(this.molecule, this.scoring) : new CombinatorialFragmenter(this.molecule);
        this.graph = fragmenter.createCombinatorialFragmentationGraph(this.bondsToCut, this.fragmentationConstraint);
        if(this.graph.getNodes().size() <= this.numFragments){
            this.graph.getNodes().forEach(node -> this.fragments.add(node.getFragment()));
            return this.graph;
        }

        // Sort all fragments (except the root) according to their total score:
        List<CombinatorialNode> sortedNodes = this.graph.getNodes().stream().sorted((node1, node2) -> {
            double score1 = node1.getTotalScore();
            double score2 = node2.getTotalScore();
            return (int) Math.signum(score2 - score1);
        }).toList();

        // Tag the first 'this.numFragments' nodes in 'sortedNodes' and all nodes on the path to the root:
        this.graph.getRoot().setState((byte) 1);
        this.graph.getNodes().forEach(node -> node.setState((byte) 0));
        for(int i = 0; i < this.numFragments; i++){
            CombinatorialNode node = sortedNodes.get(i);
            this.fragments.add(node.getFragment());
            node.setState((byte) 1);

            // tag all nodes on the path to the root:
            ArrayDeque<CombinatorialNode> queue = new ArrayDeque<>();
            queue.add(node);
            while(!queue.isEmpty()){
                CombinatorialNode n = queue.pollFirst();
                for(CombinatorialEdge inEdge : n.getIncomingEdges()){
                    CombinatorialNode parentNode = inEdge.getSource();
                    if(parentNode.getState() == (byte) 0) {
                        parentNode.setState((byte) 1);
                        queue.add(parentNode);
                    }
                }
            }
        }

        // Remove all nodes from this.graph with state equals 0:
        ArrayList<CombinatorialNode> clonedList = new ArrayList<>(this.graph.getNodes());
        for(CombinatorialNode node : clonedList) if(node.getState() == (byte) 0) this.graph.deleteNodeDangerously(node);

        return this.graph;
    }

}
