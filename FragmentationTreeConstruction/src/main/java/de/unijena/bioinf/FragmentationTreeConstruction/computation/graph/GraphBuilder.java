package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * @author Kai Dührkop
 */
public interface GraphBuilder {
    public FragmentationGraph buildGraph(ProcessedInput input, ScoredMolecularFormula pmd);
}
