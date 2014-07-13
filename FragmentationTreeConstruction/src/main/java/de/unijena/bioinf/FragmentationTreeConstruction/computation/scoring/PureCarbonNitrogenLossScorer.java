package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class PureCarbonNitrogenLossScorer implements LossScorer {

    private double penalty;

    public PureCarbonNitrogenLossScorer() {
        this(Math.log(0.1));
    }

    public PureCarbonNitrogenLossScorer(double penalty) {
        this.penalty = penalty;
    }

    @Override
    public Element prepare(ProcessedInput inputh) {
        return PeriodicTable.getInstance().getByName("N");
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object n) {
        final MolecularFormula f = loss.getFormula();
        final int nitrogen = f.numberOf((Element) n);
        final int carbon = f.numberOfCarbons();
        if (nitrogen + carbon >= f.atomCount()) return penalty;
        else return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        penalty = document.getDoubleFromDictionary(dictionary, "penalty");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "penalty", penalty);
    }
}
