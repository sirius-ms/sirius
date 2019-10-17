package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

/**
 * Quick fix for multimeres.
 * I would suggest to even "enforce" these kind of edges as they are quite obvious and should speed up computation a lot
 */
public class MultimereLossScorer implements LossScorer<Object> {
    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        if (loss.getFormula().equals(loss.getTarget().getFormula())) {
            if (loss.getSource().isRoot()) return 10d;
            else return 2d;
        } else {
            return 0d;
        }
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
