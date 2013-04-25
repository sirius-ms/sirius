package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.HashMap;
import java.util.Map;

public class FreeRadicalEdgeScorer implements LossScorer {

    private final Map<MolecularFormula, Double> freeRadicals;
    private final double generalRadicalScore;

    public static FreeRadicalEdgeScorer getRadicalScorerWithDefaultSet(double knownRadicalScore, double generalRadicalScore) {
        final MolecularFormula[] formulas = new MolecularFormula[]{
        		MolecularFormula.parse("H"), MolecularFormula.parse("O"), MolecularFormula.parse("OH"), 
        		MolecularFormula.parse("CH3"), MolecularFormula.parse("CH3O"),
        		MolecularFormula.parse("C3H7"), MolecularFormula.parse("C4H9"), 
        		MolecularFormula.parse("C6H5O")
        };
        final HashMap<MolecularFormula, Double> radicals = new HashMap<MolecularFormula, Double>(formulas.length*2);
        for (MolecularFormula formula : formulas) {
            radicals.put(formula, knownRadicalScore);
        }
        return new FreeRadicalEdgeScorer(radicals, generalRadicalScore);
    }

    public FreeRadicalEdgeScorer(Map<MolecularFormula, Double> freeRadicals, double generalRadicalScore) {
        this.freeRadicals = new HashMap<MolecularFormula, Double>(freeRadicals);
        this.generalRadicalScore = generalRadicalScore;
    }

    public void addRadical(MolecularFormula formula, double logScore) {
        freeRadicals.put(formula, logScore);
    }

    @Override
    public Object prepare(ProcessedInput input, FragmentationPathway graph) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object _) {
        // is known?
        final Double score = freeRadicals.get(loss.getLoss());
        if (score != null) return score.doubleValue();
        if (loss.getLoss().maybeCharged()) return generalRadicalScore;
        return 0d;
    }
}
