package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.ParameterHelper;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * Special elements like F, I, S, P, Cl, Br and so on are rarely and losses containing this elements seems to be
 * often special common losses. This scorer does not penalize strange elements in general, but only strange
 * elements in uncommon losses. So remark that you add this score to the CommonLossScorer to compensate this
 * penalty.
 */
public class StrangeElementScorer implements LossScorer, MolecularFormulaScorer {

    public static final double LEARNED_PENALTY = -1.9176802031231173d;
    public static final double LEARNED_NORMALIZATION = -0.13929596343581177d;

    private double penalty;
    private double normalization;

    public StrangeElementScorer() {
        this(LEARNED_PENALTY, LEARNED_NORMALIZATION);
    }

    public StrangeElementScorer(double penalty, double normalization) {
        this.penalty = penalty;
        this.normalization = normalization;
    }

    public double getPenalty() {
        return penalty;
    }

    public double getNormalization() {
        return normalization;
    }

    public StrangeElementScorer(double penalty) {
        this.penalty = penalty;
        this.normalization = 0d;
    }

    @Override
    public Object prepare(ProcessedInput inputh) {
        final PeriodicTable pt = PeriodicTable.getInstance();
        return pt.getAllByName("C", "H", "N", "O");
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getLoss(), (Element[])precomputed);
    }

    public boolean containsStrangeElement(MolecularFormula formula) {
        return formula.numberOfCarbons() + formula.numberOfHydrogens() + formula.numberOfOxygens() + formula.numberOf(PeriodicTable.getInstance().getByName("N")) >= formula.atomCount();
    }

    private double score(MolecularFormula loss, Element[] precomputed) {
        final boolean isChnops = loss.numberOf(precomputed[0]) + loss.numberOf(precomputed[1]) + loss.numberOf(precomputed[2]) + loss.numberOf(precomputed[3]) >= loss.atomCount();
        return (isChnops ? 0d : penalty) - normalization;
    }

    @Override
    public double score(MolecularFormula formula) {
        final boolean isChnops =  containsStrangeElement(formula);
        return (isChnops ? 0d : penalty) - normalization;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        penalty = document.getDoubleFromDictionary(dictionary, "penalty");
        normalization = document.getDoubleFromDictionary(dictionary, "normalization");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "penalty", penalty);
        document.addToDictionary(dictionary, "normalization", normalization);
    }
}
