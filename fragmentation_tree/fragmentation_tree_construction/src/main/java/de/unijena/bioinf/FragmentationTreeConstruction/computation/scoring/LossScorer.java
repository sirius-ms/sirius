
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

/**
 * @author Kai Dührkop
 */
public interface LossScorer<T> extends Parameterized {

    T prepare(ProcessedInput input, AbstractFragmentationGraph graph);

    double score(Loss loss, ProcessedInput input, T precomputed);

    /*
    if true, this scorer is called for ALL edges
    if false, this scorer is only called for edges that correspond to an ordinary fragmentation reaction
     */
    default boolean processArtificialEdges() {
        return false;
    }

}
