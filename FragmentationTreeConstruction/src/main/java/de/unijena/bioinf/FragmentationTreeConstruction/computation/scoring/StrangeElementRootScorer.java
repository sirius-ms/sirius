package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

/**
 * penalize a compound for each different strange (non CHNO) element in its formula.
 * Remark: You should add a StrangeElementInCommonLossScorer and a StrangeElementInSmallFragments scorer
 * to compensate this penality
 */
public class StrangeElementRootScorer implements DecompositionScorer<Element[]> {

    private double penalty;

    private Element C, H, N, O;

    public StrangeElementRootScorer() {
        this(Math.log(0.25));
    }

    public StrangeElementRootScorer(double penalty) {
        this.penalty = penalty;
    }

    public double getPenalty() {
        return penalty;
    }

    public void setPenalty(double penalty) {
        this.penalty = penalty;
    }

    @Override
    public Element[] prepare(ProcessedInput input) {
        final PeriodicTable p = PeriodicTable.getInstance();
        C = p.getByName("C"); H = p.getByName("H"); N = p.getByName("N"); O = p.getByName("O");
        return new Element[]{p.getByName("Na"), p.getByName("K"), p.getByName("Cl"), p.getByName("Br")};
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Element[] precomputed) {
        if (!formula.isCHNO()) {
            final double[] score = new double[1];
            formula.visit(new FormulaVisitor<Object>() {
                @Override
                public Object visit(Element element, int amount) {
                    if (amount > 0 && element!=C && element!=H && element!=N && element!=O) {
                        score[0] += penalty;
                    }
                    return null;
                }
            });
            assert !Double.isNaN(score[0]);
            final Ionization ion = input.getExperimentInformation().getIonization();
            if (ion instanceof Charge) {
                final int i = (input.getExperimentInformation().getIonization().getCharge() > 0) ? 0 : 2;
                for (int j=0; j < 2; ++j) {
                    if (formula.numberOf(precomputed[j+i]) == ion.chargeNumber()) {
                        score[0] -= penalty;
                        break;
                    }
                }
            }
            return score[0];
        }
        return 0d;
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
