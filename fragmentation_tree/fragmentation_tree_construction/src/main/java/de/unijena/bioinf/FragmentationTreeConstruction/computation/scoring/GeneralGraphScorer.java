package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.sirius.ProcessedInput;

public interface GeneralGraphScorer {

    public double score(AbstractFragmentationGraph graph, ProcessedInput input);

}
