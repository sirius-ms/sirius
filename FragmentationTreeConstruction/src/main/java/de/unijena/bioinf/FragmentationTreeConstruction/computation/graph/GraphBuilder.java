package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

/**
 * @author Kai Dührkop
 */
public interface GraphBuilder {
    public FGraph initializeEmptyGraph(ProcessedInput input);

    public FGraph addRoot(FGraph graph, ProcessedPeak peak, Iterable<ScoredMolecularFormula> pmds);

    public FGraph fillGraph(FGraph graph);
}
