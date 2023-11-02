package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import org.openscience.cdk.interfaces.IBond;

import java.util.*;

/**
 * An instance of this class fragments a given molecular structure according to the specified
 * {@link FragmentationRules fragmentation rules} and {@link CombinatorialFragmenter.Callback2 fragmentation constraint}.
 * After this rule-based fragmentation, a certain number of the best fragments are selected
 * respective to the specified {@link CombinatorialFragmenterScoring scoring}.
 * The resulting {@link CombinatorialGraph} is induced by this set of selected/predicted fragments.
 */
public class RuleBasedFragmentationPredictor extends AbstractFragmentationPredictor{

    /**
     * The scoring function to weigh each edge and node in the generated {@link CombinatorialGraph}.
     */
    private final CombinatorialFragmenterScoring scoring;

    /**
     * The fragmentation constraint which determines which fragments will be further fragmented.
     */
    private final CombinatorialFragmenter.Callback2 fragmentationConstraint;

    /**
     * A {@link BitSet} object indicating which bonds in the given molecule will be cut during fragmentation.
     */
    private final BitSet bondsToCut;

    /**
     * The number of fragments which will be generated during fragmentation simulation.
     */
    private final int numFragments;

    /**
     * Initialises a {@link RuleBasedFragmentationPredictor} object.<br>
     * {@code fragmentationRules} is used for iterating over all bonds of {@code molecule},
     * and decide whether a certain bond will be cut during fragmentation or not.
     *
     * @param molecule the molecular structure for which the fragments will be predicted
     * @param scoring the scoring function for generating the {@link CombinatorialGraph},
     *                and which determines which fragments will be chosen
     * @param numFragments the number of generated fragments (excluding the intact molecule)
     * @param fragmentationRules an object of {@link FragmentationRules} specifying which bonds in {@code molecule} to cut
     * @param fragmentationConstraint the fragmentation constraint which determines which fragments will be further fragmented
     */
    public RuleBasedFragmentationPredictor(MolecularGraph molecule, CombinatorialFragmenterScoring scoring, int numFragments, FragmentationRules fragmentationRules, CombinatorialFragmenter.Callback2 fragmentationConstraint){
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

    /**
     * Initialises a {@link RuleBasedFragmentationPredictor} object which uses a default scoring.
     * In this scoring function each broken bond is assigned with a score of -1 and each node has a score of 0;
     * i.e. the length of a path from root to a fragment is equal to {@code -numberBrokenBonds}. <br>
     * {@code fragmentationRules} is used for iterating over all bonds of {@code molecule},
     * and decide whether a certain bond will be cut during fragmentation or not.
     *
     * @param molecule the molecular structure for which the fragments will be predicted
     * @param numFragments the number of generated fragments (excluding the intact molecule)
     * @param fragmentationRules an object of {@link FragmentationRules} specifying which bonds in {@code molecule} to cut
     * @param fragmentationConstraint the fragmentation constraint which determines which fragments will be further fragmented
     */
    public RuleBasedFragmentationPredictor(MolecularGraph molecule, int numFragments, FragmentationRules fragmentationRules, CombinatorialFragmenter.Callback2 fragmentationConstraint){
        this(molecule, null, numFragments, fragmentationRules, fragmentationConstraint);
    }

    public RuleBasedFragmentationPredictor(MolecularGraph molecule, CombinatorialFragmenterScoring scoring, FragmentationRules fragmentationRules, CombinatorialFragmenter.Callback2 fragmentationConstraint){
        this(molecule, scoring, Integer.MAX_VALUE, fragmentationRules, fragmentationConstraint);
    }

    public RuleBasedFragmentationPredictor(MolecularGraph molecule, FragmentationRules fragmentationRules, CombinatorialFragmenter.Callback2 fragmentationConstraint){
        this(molecule, null, Integer.MAX_VALUE, fragmentationRules, fragmentationConstraint);
    }

    @Override
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
