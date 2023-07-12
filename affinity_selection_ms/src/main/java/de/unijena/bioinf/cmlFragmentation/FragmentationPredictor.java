package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import java.util.Collection;

public abstract class FragmentationPredictor {

    private final MolecularGraph molecule;
    private final CombinatorialFragmenter fragmenter;

    public FragmentationPredictor(MolecularGraph molecule){
        this(molecule, null);
    }

    public FragmentationPredictor(MolecularGraph molecule, CombinatorialFragmenterScoring scoring){
        this.molecule = molecule;
        this.fragmenter = scoring != null ? new CombinatorialFragmenter(molecule, scoring) : new CombinatorialFragmenter(molecule);
    }

    public abstract CombinatorialGraph predictFragmentation();

    public abstract Collection<CombinatorialFragment> getFragments();

    public MolecularGraph getMolecule(){
        return this.molecule;
    }

    public CombinatorialFragmenter getFragmenter(){
        return this.fragmenter;
    }

}
