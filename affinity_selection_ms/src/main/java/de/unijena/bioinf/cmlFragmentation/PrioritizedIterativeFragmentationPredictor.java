package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import java.util.*;

/**
 * An object of this class predicts the fragmentation process for a given molecular structure
 * (occurring during MS/MS acquisition) in a prioritized breadth-first search approach.
 * The simulation of the fragmentation process is comparable to a prioritized approach
 * for generating a combinatorial fragmentation graph.<br>
 *
 * In every step, the fragment F with the most profitable path to the root is dequeued from the priority queue Q and
 * marked as predicted fragment. For this fragment F, every direct subfragment F' is generated and:
 * if F' is novel, it's added to the graph G and to the priority queue Q
 * [two options: if F' is not novel, then do nothing or maybe replace the subtree under F' to F (I have to test that...)]
 * After a certain number of fragments were predicted, the remaining (unmarked) fragments in Q are removed from G.
 */
public class PrioritizedIterativeFragmentationPredictor extends AbstractFragmentationPredictor{

    /**
     * The scoring function to weigh each edge and node in the generated {@link CombinatorialGraph}.
     */
    private final CombinatorialFragmenterScoring scoring;

    /**
     * The number of fragments which will be generated during fragmentation simulation.
     */
    private final int numFragments;

    /**
     * Constructs an object of {@link PrioritizedIterativeFragmentationPredictor}.
     *
     * @param molecule the molecular structure for which the fragmentation will be predicted
     * @param scoring the scoring function for generating the {@link CombinatorialGraph},
     *                and which determines which fragments will be chosen
     * @param numFragments the number of generated fragments (excluding the intact molecule)
     */
    public PrioritizedIterativeFragmentationPredictor(MolecularGraph molecule, CombinatorialFragmenterScoring scoring, int numFragments){
        super(molecule);
        this.scoring = scoring;
        this.numFragments = numFragments;
    }
    @Override
    public CombinatorialGraph predictFragmentation() {
        // 1. Initialisation:
        CombinatorialFragmenter fragmenter = new CombinatorialFragmenter(this.molecule, this.scoring);
        this.graph = new CombinatorialGraph(this.molecule);
        PriorityQueue<CombinatorialNode> queue = new PriorityQueue<>((node1,node2) -> {
           float score1 = node1.getTotalScore();
           float score2 = node2.getTotalScore();
           return (int) Math.signum(score2 - score1);
        });

        // 1.2: Create all direct fragments of the root and put them into this.graph.
        // Set their status on '0' and put them into the queue.
        CombinatorialNode root = this.graph.getRoot();
        root.setState((byte) 1);
        fragmenter.cutAllBonds(root.getFragment(), (parent, bonds, fragments1) -> {
           for(CombinatorialFragment fragment : fragments1){
               CombinatorialNode w = this.graph.addReturnAlways(root, fragment, bonds[0], bonds.length > 1 ? bonds[1] : null, this.scoring, null);
               w.setState((byte) 0);
               queue.add(w);
           }
        });

        // 2. Loop:
        int currentNumFragments = 0;
        while(currentNumFragments < this.numFragments && !queue.isEmpty()){
            CombinatorialNode node = queue.poll();
            node.setState((byte) 1);
            currentNumFragments++;

            // todo: this is the part of the code where I can try out new ideas
            fragmenter.cutAllBonds(node.getFragment(), (parent, bonds, fragments1) -> {
                for(CombinatorialFragment fragment : fragments1){
                    if(!this.graph.contains(fragment)){
                        CombinatorialNode w = this.graph.addReturnAlways(node, fragment, bonds[0], bonds.length > 1 ? bonds[1] : null, this.scoring, null);
                        w.setState((byte) 0);
                        queue.add(w);
                    }
                }
            });
        }

        // 3. Remove all nodes with state equals 0:
        ArrayList<CombinatorialNode> clonedNodeList = new ArrayList<>(this.graph.getNodes());
        clonedNodeList.forEach(node -> {
            if(node.getState() == (byte) 0) this.graph.deleteNodeDangerously(node);
        });

        // 4. Add the remaining nodes into the list this.fragments:
        this.graph.getNodes().forEach(node -> this.fragments.add(node.getFragment()));

        return this.graph;
    }
}
