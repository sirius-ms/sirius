package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Called("Free Radical")
public class FreeRadicalEdgeScorer implements LossScorer, MolecularFormulaScorer {

    private final Map<MolecularFormula, Double> freeRadicals;
    private double generalRadicalScore;
    private double normalization;

    public static FreeRadicalEdgeScorer getRadicalScorerWithDefaultSet() {
        return getRadicalScorerWithDefaultSet(Math.log(0.9), Math.log(0.1), -0.011626542158820332d);
    }

    public static FreeRadicalEdgeScorer getRadicalScorerWithDefaultSet(double knownRadicalScore, double generalRadicalScore, double normalization) {
        final MolecularFormula[] formulas = new MolecularFormula[]{
        		MolecularFormula.parse("H"), MolecularFormula.parse("O"), MolecularFormula.parse("OH"), 
        		MolecularFormula.parse("CH3"), MolecularFormula.parse("CH3O"),
        		MolecularFormula.parse("C3H7"), MolecularFormula.parse("C4H9"), 
        		MolecularFormula.parse("C6H5O"), MolecularFormula.parse("C6H5"), MolecularFormula.parse("C6H6N"), MolecularFormula.parse("I")
        };
        final HashMap<MolecularFormula, Double> radicals = new HashMap<MolecularFormula, Double>(formulas.length*2);
        for (MolecularFormula formula : formulas) {
            radicals.put(formula, knownRadicalScore);
        }
        return new FreeRadicalEdgeScorer(radicals, generalRadicalScore, normalization);
    }

    public FreeRadicalEdgeScorer() {
        this.freeRadicals = new HashMap<MolecularFormula, Double>();
        this.generalRadicalScore = 0d;
        this.normalization = 0d;
    }

    public FreeRadicalEdgeScorer(Map<MolecularFormula, Double> freeRadicals, double generalRadicalScore, double normalization) {
        this.freeRadicals = new HashMap<MolecularFormula, Double>(freeRadicals);
        this.generalRadicalScore = generalRadicalScore;
        this.normalization = normalization;
    }

    public double getGeneralRadicalScore() {
        return generalRadicalScore;
    }

    public void setGeneralRadicalScore(double generalRadicalScore) {
        this.generalRadicalScore = generalRadicalScore;
    }

    public double getNormalization() {
        return normalization;
    }

    public void setNormalization(double normalization) {
        this.normalization = normalization;
    }

    public void addRadical(MolecularFormula formula, double logScore) {
        freeRadicals.put(formula, logScore);
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object _) {
        return score(loss.getLoss()) - normalization;
    }

    @Override
    public double score(MolecularFormula formula) {
        final Double score = freeRadicals.get(formula);
        if (score != null) return score.doubleValue();
        if (formula.maybeCharged()) return generalRadicalScore;
        return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final FreeRadicalEdgeScorer edgeScorer = new FreeRadicalEdgeScorer();
        final Iterator<Map.Entry<String, G>> iter = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "commonRadicals"));
        while (iter.hasNext()) {
            final Map.Entry<String,G> v = iter.next();
            edgeScorer.addRadical(MolecularFormula.parse(v.getKey()), (Double)v.getValue());
        }
        edgeScorer.setGeneralRadicalScore(document.getDoubleFromDictionary(dictionary, "radicalPenalty"));
        edgeScorer.setNormalization(document.getDoubleFromDictionary(dictionary, "normalization"));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final D radicals = document.newDictionary();
        for (MolecularFormula f : freeRadicals.keySet()) {
            document.addToDictionary(radicals, f.toString(), freeRadicals.get(f).doubleValue());
        }
        document.addDictionaryToDictionary(dictionary, "commonRadicals", radicals);
        document.addToDictionary(dictionary, "radicalPenalty", generalRadicalScore);
        document.addToDictionary(dictionary, "normalization",  normalization);

    }
}
