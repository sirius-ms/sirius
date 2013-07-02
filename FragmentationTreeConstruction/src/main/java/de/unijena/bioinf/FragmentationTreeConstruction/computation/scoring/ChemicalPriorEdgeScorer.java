package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.ParameterHelper;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class ChemicalPriorEdgeScorer implements LossScorer {

    private MolecularFormulaScorer prior;
    private double normalization;

    public ChemicalPriorEdgeScorer(MolecularFormulaScorer prior, double normalization) {
        this.prior = prior;
        this.normalization = normalization;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final double child = Math.max(Math.log(0.0001), prior.score(loss.getTail().getDecomposition().getFormula()));
        final double parent = Math.max(Math.log(0.0001), prior.score(loss.getHead().getDecomposition().getFormula()));
        return Math.min(0, child - parent) - normalization;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.prior = (MolecularFormulaScorer)helper.unwrap(document, document.getFromDictionary(dictionary, "prior"));
        this.normalization = document.getDoubleFromDictionary(dictionary, "normalization");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "prior", helper.wrap(document, prior));
        document.addToDictionary(dictionary, "normalization", normalization);
    }
}
