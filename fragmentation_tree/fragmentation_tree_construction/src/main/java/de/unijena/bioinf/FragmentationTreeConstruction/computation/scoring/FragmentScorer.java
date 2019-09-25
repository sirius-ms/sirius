package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

/**
 * A scorer for each fragment node in the graph
 */
public interface FragmentScorer<T> {

    public T prepare(ProcessedInput input, AbstractFragmentationGraph graph);

    public double score(Fragment graphFragment, ProcessedPeak correspondingPeak, boolean isRoot, T prepared);

}
