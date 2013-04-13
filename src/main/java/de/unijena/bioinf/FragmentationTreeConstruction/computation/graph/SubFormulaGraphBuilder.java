package de.unijena.bioinf.FragmentationTreeConstruction.computation.graph;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.GraphFragment;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Kai Dührkop
 */
public class SubFormulaGraphBuilder implements GraphBuilder {
    @Override
    public FragmentationGraph buildGraph(ProcessedInput input, ScoredMolecularFormula pmd) {
        final ProcessedPeak parentPeak = input.getParentPeak();
        final FragmentationGraph graph = new FragmentationGraph(input);
        final GraphFragment root = graph.addVertex(parentPeak, pmd);
        final ArrayList<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        Collections.sort(peaks, new ProcessedPeak.MassComparator());
        int n = peaks.indexOf(parentPeak);
        for (int i = n-1; i >= 0; --i) {
            final ProcessedPeak peak = peaks.get(i);
            final int pi = peak.getIndex();
            for (ScoredMolecularFormula decomposition : peak.getDecompositions()) {
                final MolecularFormula formula = decomposition.getFormula();
                final boolean hasEdge = pmd.getFormula().isSubtractable(formula);
                if (hasEdge) {
                    final GraphFragment newFragment = graph.addVertex(peak, decomposition);
                    graph.addEdge(root, newFragment);
                    for (GraphFragment f : graph.getFragmentsWithoutRoot()) {
                        if (f.getPeak().getIndex() == pi) continue;
                        final MolecularFormula fragmentFormula = f.getDecomposition().getFormula();
                        assert  (f.getPeak().getMz() > peak.getMz());
                        if (fragmentFormula.isSubtractable(formula)) {
                            graph.addEdge(f, newFragment);
                        }
                    }
                }
            }
        }
        return graph;
    }
}
