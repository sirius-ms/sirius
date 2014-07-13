package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class ChemicalPriorEdgeScorer implements LossScorer {

    private MolecularFormulaScorer prior;
    private double normalization;
    private double minimalMass;

    public ChemicalPriorEdgeScorer() {
        this(ChemicalCompoundScorer.createDefaultCompoundScorer(true), 0d, 100d);
    }

    public ChemicalPriorEdgeScorer(MolecularFormulaScorer prior, double normalization, double minimalMass) {
        this.prior = prior;
        this.normalization = normalization;
        this.minimalMass = minimalMass;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getSource().getFormula(), loss.getTarget().getFormula());
    }

    public double score(MolecularFormula parentFormula, MolecularFormula childFormula) {
        if (childFormula.getMass() < 100d) return 0d;
        final double child = Math.max(Math.log(0.0001), prior.score(childFormula));
        final double parent = Math.max(Math.log(0.0001), prior.score(parentFormula));
        return Math.min(0, child - parent) - normalization;
    }

    public MolecularFormulaScorer getPrior() {
        return prior;
    }

    public void setPrior(MolecularFormulaScorer prior) {
        this.prior = prior;
    }

    public double getMinimalMass() {
        return minimalMass;
    }

    public void setMinimalMass(double minimalMass) {
        this.minimalMass = minimalMass;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.prior = (MolecularFormulaScorer) helper.unwrap(document, document.getFromDictionary(dictionary, "prior"));
        this.normalization = document.getDoubleFromDictionary(dictionary, "normalization");
        this.minimalMass = document.getDoubleFromDictionary(dictionary, "minimalMass");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "prior", helper.wrap(document, prior));
        document.addToDictionary(dictionary, "normalization", normalization);
        document.addToDictionary(dictionary, "minimalMass", minimalMass);
    }
}
