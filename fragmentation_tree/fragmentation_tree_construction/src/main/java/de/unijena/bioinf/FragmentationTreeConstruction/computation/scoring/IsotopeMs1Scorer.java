package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ExtractedIsotopePattern;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

public class IsotopeMs1Scorer implements DecompositionScorer<IsotopeMs1Scorer.Prepared> {

    protected static class Prepared {
        private final IsotopeScoring weight;
        private final ExtractedIsotopePattern pattern;

        public Prepared(IsotopeScoring weight, ExtractedIsotopePattern pattern) {
            this.weight = weight;
            this.pattern = pattern;
        }
    }

    @Override
    public Prepared prepare(ProcessedInput input) {
        return new Prepared(input.getAnnotationOrDefault(IsotopeScoring.class), input.getAnnotation(ExtractedIsotopePattern.class, null));
    }

    @Override
    public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Prepared precomputed) {
        if (precomputed.pattern!=null && precomputed.pattern.getExplanations().get(formula)!=null) {
            return precomputed.weight.getIsotopeScoreWeighting() * precomputed.pattern.getExplanations().get(formula).getScore();
        } else return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
