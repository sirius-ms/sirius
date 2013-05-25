package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import static de.unijena.bioinf.FragmentationTreeConstruction.inspection.Inspectable.Utils.keyValues;

@Called("Chemical Prior")
public class ChemicalPriorScorer implements DecompositionScorer<Object> {

    public static final double LEARNED_NORMALIZATION_CONSTANT = 0.17546357436139415d;
    private final MolecularFormulaScorer prior;
    private double normalizationConstant;

    public ChemicalPriorScorer() {
        this(ChemicalCompoundScorer.createDefaultCompoundScorer(), LEARNED_NORMALIZATION_CONSTANT);
    }

    public ChemicalPriorScorer(MolecularFormulaScorer prior, double normalizationConstant) {
        this.prior = prior;
        this.normalizationConstant = normalizationConstant;
    }


    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
        return prior.score(formula) - normalizationConstant;
    }
}
