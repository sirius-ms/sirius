package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.ArrayList;
import java.util.List;

public class PenalizeAdductLossScorer implements EdgeScorer{

    private final MolecularFormula[][] forbiddenFormulasPositive;
    private final MolecularFormula[][] forbiddenFormulasNegative;
    private final double singleScore, doubleScore;

    public PenalizeAdductLossScorer() {
        this.forbiddenFormulasPositive = new MolecularFormula[][]{{MolecularFormula.parse("Na"), MolecularFormula.parse("K")}};
        this.forbiddenFormulasNegative = new MolecularFormula[][]{{MolecularFormula.parse("Cl")}};
        this.singleScore = Math.log(0.1);
        this.doubleScore = Math.log(0.5);
    }

    @Override
    public Object prepare(ProcessedInput input, FragmentationPathway graph) {
        if (!(input.getOriginalInput().getStandardIon() instanceof Charge)) return null;
        final ArrayList<ScoredMolecularFormula> formulas = new ArrayList<ScoredMolecularFormula>();
        final MolecularFormula decomp = graph.getRoot().getDecomposition().getFormula();
        final int charge = input.getOriginalInput().getStandardIon().getCharge();
        final MolecularFormula[][] array = charge > 0 ? forbiddenFormulasPositive : forbiddenFormulasNegative;
        final int chargeNum = input.getOriginalInput().getStandardIon().chargeNumber();
        if (array.length < chargeNum) return null;
        for (MolecularFormula f : array[chargeNum-1]) {
            if (decomp.isSubtractable(f)) {
                if (decomp.isSubtractable(f.multiply(2))) {
                    formulas.add(new ScoredMolecularFormula(f, doubleScore));
                    formulas.add(new ScoredMolecularFormula(f.multiply(2), singleScore));
                } else {
                    formulas.add(new ScoredMolecularFormula(f, singleScore));
                }
            }
        }
        return formulas;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final List<ScoredMolecularFormula> formulas = (List<ScoredMolecularFormula>)precomputed;
        if (formulas == null) return 0d;
        double penality = 0d;
        for (ScoredMolecularFormula f : formulas)
            if (loss.getLoss().isSubtractable(f.getFormula())) penality = Math.min(penality, f.getScore());
        return penality;
    }
}
