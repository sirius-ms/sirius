package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.*;

public class MostLikelyFragmentation extends AbstractFragmentationPredictor{

    private final CombinatorialFragmenterScoring scoring;
    private final int numFragments;
    public MostLikelyFragmentation(MolecularGraph molecule, CombinatorialFragmenterScoring scoring, int numFragments){
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

            fragmenter.cutAllBonds(node.getFragment(), (parent, bonds, fragments1) -> {
                for(CombinatorialFragment fragment : fragments1){
                    if(this.graph.contains(fragment)){
                        CombinatorialNode w = this.graph.addReturnAlways(node, fragment, bonds[0], bonds.length > 1 ? bonds[1] : null, this.scoring, null);
                        if(w.getState() == (byte) 0){
                            queue.remove(w); queue.add(w); // update the queue because w.totalScore has possibly changed
                        }
                    }else{
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
