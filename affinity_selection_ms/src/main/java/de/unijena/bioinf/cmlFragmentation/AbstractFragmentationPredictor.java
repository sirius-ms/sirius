package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This abstract class is a shallow implementation of {@link FragmentationPredictor}.
 */
public abstract class AbstractFragmentationPredictor implements FragmentationPredictor{

    /**
     * The molecular structure for which the fragmentation is predicted.
     */
    protected final MolecularGraph molecule;

    /**
     * The list containing all predicted fragments of {@link AbstractFragmentationPredictor#molecule}.
     * Note that the intact molecule itself is not contained.
     */
    protected final ArrayList<CombinatorialFragment> fragments; // fragments of 'molecule' without 'molecule'

    /**
     * The {@link CombinatorialGraph} representing the predicted fragmentation process.
     */
    protected CombinatorialGraph graph;

    /**
     * Initialises an object of a class which is extending {@link AbstractFragmentationPredictor}.
     *
     * @param molecule the molecular structure those fragmentation process will be predicted
     */
    public AbstractFragmentationPredictor(MolecularGraph molecule){
        this.molecule = molecule;
        this.fragments = new ArrayList<>();
    }

    @Override
    public List<CombinatorialFragment> getFragments(){
        return this.fragments;
    }

    /**
     * Returns a list of all predicted fragments including the intact molecule.
     *
     * @return list of all predicted fragments including the intact molecule
     */
    public List<CombinatorialFragment> getFragmentsWithPrecursor(){
        ArrayList<CombinatorialFragment> allFragments = new ArrayList<>(this.fragments);
        allFragments.add(this.molecule.asFragment());
        return allFragments;
    }

    @Override
    public CombinatorialGraph getFragmentationGraph(){
        return this.graph;
    }

    @Override
    public MolecularGraph getMolecule(){
        return this.molecule;
    }

}
