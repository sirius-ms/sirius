package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialGraph;
import de.unijena.bioinf.fragmenter.MolecularGraph;

import java.util.List;

/**
 * A FragmentationPredictor predicts the fragmentation process of a given molecular structure
 * in the collision cell of an MS/MS instrument.
 */
public interface FragmentationPredictor {

    /**
     * Returns the molecular structure for which the fragmentation process is predicted.
     *
     * @return a {@link MolecularGraph} object representing this molecular structure
     */
    MolecularGraph getMolecule();

    /**
     * This method simulates the fragmentation process occurring in the collision cell.
     * A {@link de.unijena.bioinf.fragmenter.CombinatorialGraph} will be returned which contains the predicted
     * fragmentation pathways. The nodes of this graph are fragments of the molecule and
     * there is a directed edge between fragment F and F' if F' is a direct fragment of F.
     *
     * @return the predicted fragmentation graph
     */
    CombinatorialGraph predictFragmentation();

    /**
     * Returns a list of predicted {@link CombinatorialFragment fragments} which can lead to a peak.<br>
     * Note that all of these fragments should be contained in the predicted {@link CombinatorialGraph}.
     * But not necessarily all fragments in the predicted fragmentation graph have to be included in this list
     * since not every produced fragment leads to a peak.
     *
     * Furthermore, this method can only be called after calling {@link FragmentationPredictor#predictFragmentation()}.
     *
     * @return list of predicted fragments which should lead to a peak
     */
    List<CombinatorialFragment> getFragments();

    /**
     * Returns the predicted {@link CombinatorialGraph fragmentation graph} which was constructed after calling
     * {@link FragmentationPredictor#predictFragmentation()}.
     *
     * @return a {@link CombinatorialGraph} representing the simulated fragmentation graph
     */
    CombinatorialGraph getFragmentationGraph();


}
