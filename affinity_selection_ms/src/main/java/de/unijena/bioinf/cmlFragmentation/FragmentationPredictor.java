package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialGraph;
import de.unijena.bioinf.fragmenter.MolecularGraph;

import java.util.List;

public interface FragmentationPredictor {

    MolecularGraph getMolecule();
    CombinatorialGraph predictFragmentation();
    List<CombinatorialFragment> getFragments();
    CombinatorialGraph getFragmentationGraph();


}
