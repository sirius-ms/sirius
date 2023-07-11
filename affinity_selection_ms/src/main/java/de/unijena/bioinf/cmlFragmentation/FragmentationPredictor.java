package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import java.util.Collection;

public abstract class FragmentationPredictor {

    private final MolecularGraph molecule;
    private final CombinatorialFragmenter fragmenter;
    private CombinatorialGraph graph;

    public FragmentationPredictor(MolecularGraph molecule){
        this.molecule = molecule;
        this.fragmenter = new CombinatorialFragmenter(molecule);
    }

    public FragmentationPredictor(MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        this.molecule = molecule;
        this.fragmenter = new CombinatorialFragmenter(molecule, scoring);
    }

    public abstract CombinatorialGraph predictFragmentation();

    public abstract Collection<CombinatorialFragment> getFragments();

    public CombinatorialGraph getFragmentationGraph(){
        return this.graph;
    }

    public MolecularGraph getMolecule(){
        return this.molecule;
    }

}
