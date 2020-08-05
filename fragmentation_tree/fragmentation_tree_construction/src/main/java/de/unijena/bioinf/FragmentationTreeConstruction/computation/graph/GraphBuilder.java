
package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.Set;

/**
 * @author Kai Dührkop
 */
public interface GraphBuilder {
    FGraph initializeEmptyGraph(ProcessedInput input);

    FGraph addRoot(FGraph graph, ProcessedPeak peak, Iterable<Decomposition> pmds);

    FGraph fillGraph(ProcessedInput input, FGraph graph, Set<Ionization> allowedIonModes, LossValidator validator);
}
