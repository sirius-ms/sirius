package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;

public class ChemicalPriorScorer implements DecompositionScorer<Object> {

    private final MolecularFormulaScorer prior;

    public ChemicalPriorScorer() {
        this(ChemicalCompoundScorer.createDefaultCompoundScorer());
    }

    public ChemicalPriorScorer(MolecularFormulaScorer prior) {
        this.prior = prior;
    }


    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
        return prior.score(formula);
    }
}
