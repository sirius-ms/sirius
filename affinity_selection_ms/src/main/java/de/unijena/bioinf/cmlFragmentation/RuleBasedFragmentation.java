package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import org.openscience.cdk.interfaces.IBond;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

public class RuleBasedFragmentation extends FragmentationPredictor{

    private CombinatorialGraph graph;
    private final CombinatorialFragmenter.Callback2 fragmentationConstraint;
    private final BitSet bondsToCut;

    public RuleBasedFragmentation(MolecularGraph molecule, FragmentationRules fragmentationRules, CombinatorialFragmenter.Callback2 fragmentationConstraint){
        super(molecule);
        this.fragmentationConstraint = fragmentationConstraint;

        this.bondsToCut = new BitSet(molecule.getBonds().length);
        for(IBond bond : molecule.getBonds()){
            int bondIdx = bond.getIndex();
            bondsToCut.set(bondIdx, fragmentationRules.match(bond));
        }
    }

    @Override
    public CombinatorialGraph predictFragmentation() {
        CombinatorialFragmenter fragmenter = this.getFragmenter();
        this.graph = fragmenter.createCombinatorialFragmentationGraph(this.bondsToCut, this.fragmentationConstraint);
        return this.graph;
    }

    @Override
    public Collection<CombinatorialFragment> getFragments() {
        ArrayList<CombinatorialFragment> fragments = new ArrayList<>(this.graph.numberOfNodes());
        fragments.add(this.graph.getRoot().getFragment());
        for(CombinatorialNode node : this.graph.getNodes()){
            fragments.add(node.getFragment());
        }
        return fragments;
    }
}
