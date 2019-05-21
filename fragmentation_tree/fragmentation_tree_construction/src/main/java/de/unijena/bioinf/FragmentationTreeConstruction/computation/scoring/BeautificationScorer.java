package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Beautified;
import de.unijena.bioinf.sirius.ProcessedInput;

/**
  This removes all Bonus scores we have added via the treesize scorer from the tree. Doing so will
 give the tree the same score as before, even if we have increased tree size to explain more peaks.
 */
public class BeautificationScorer implements GeneralGraphScorer{
    @Override
    public double score(AbstractFragmentationGraph graph, ProcessedInput input) {
        return -graph.getAnnotation(Beautified.class, Beautified::ugly).getBeautificationPenalty();
    }
}
